

import java.awt.FlowLayout;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.sql.Blob;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

/**
 * 
 * @author Elijah Kaufman
 * @version 1.0
 * @description Uses opencv and network table 3.0 to detect the vision targets
 *
 */
public class TowerTracker {

	/**
	 * static method to load opencv and networkTables
	 */
	static{ 
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		try{
			NetworkTable.setClientMode();
			NetworkTable.setIPAddress("10.6.68.2");
		}
		catch (Exception e ){

		}
	}
	//	constants for the color rbg values
	public static final Scalar 
	RED = new Scalar(0, 0, 255),
	BLUE = new Scalar(255, 0, 0),
	GREEN = new Scalar(0, 255, 0),
	BLACK = new Scalar(255,255,255),
	YELLOW = new Scalar(0, 255, 255),
	//	these are the threshold values in order 
	LOWER_BOUNDS = new Scalar(200, 200, 0),
	UPPER_BOUNDS = new Scalar(255, 255, 15);
	// = new Scalar(150, 150, 0),
	//UPPER_BOUNDS = new Scalar(255,  255, 90);
	//	the size for resing the image
	public static final Size resize = new Size(320,180);

	//	ignore these
	public static VideoCapture videoCapture;
	public static Mat matStream, matGray, matOriginal, matHSV, matThresh, clusters, matHeirarchy;

	public static ImageIcon icon;
	public static JFrame frame=new JFrame();
	public static JLabel lbl=new JLabel();
	//	Constants for known variables
	//	the height to the top of the target in first stronghold is 97 inches	
	public static final int TOP_TARGET_HEIGHT = 97;
	//	the physical height of the camera lens
	public static final int TOP_CAMERA_HEIGHT = 11;

	public static final int CAP_PROP_FRAME_WIDTH = 3, CAP_PROP_FRAME_HEIGHT = 4, CAP_PROP_BRIGHTNESS = 10, CAP_PROP_CONTRST = 11, 
							CAP_PROP_SATURATION = 12, CAP_PROP_HUE = 13, CAP_PROP_GAIN = 14, CAP_PROP_EXPOSURE = 15, CAP_PROP_WHITE_BALANCE_BLUE_U = 17, 
							CAP_PROP_GAMMA = 22, CAP_PROP_WHITE_BALANCE_RED_V = 26;
	//	camera details, can usually be found on the datasheets of the camera
	public static final double VERTICAL_FOV  = 52;
	public static final double HORIZONTAL_FOV  = 82.15;
	public static final double CAMERA_ANGLE = 28.5;

	public static final double thresh = 16.0;
	public static long time;
	public static long lastTime = 0;
	public static double fps;
	
	public static double min = 400;
	public static double distMin = 0;
	
	public double[] dists, azimuths; 
	
	public static boolean shouldRun = true;

	/**
	 * 
	 * @param args command line arguments
	 * just the main loop for the program and the entry points
	 */
	public static NetworkTable table;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		matGray = new Mat();
		matOriginal = new Mat();
		matHSV = new Mat();
		matThresh = new Mat();
		clusters = new Mat();
		matHeirarchy = new Mat();
		matStream = new Mat();
		

		new Thread(){

			public void run(){
				while (true){
					try{
						//System.out.println("her");
						double[] x =  matOriginal.get(143, 452);
						displayImage( Mat2BufferedImage(matOriginal));
						//System.out.println(matHSV.get(295, 183));
						for (int i = 0; i < x.length; i++ ){
							System.out.print(x[i] + " ");
							
						}
					System.out.println("");
////
					}

					catch(Exception e){

					}

				}
			}
			public BufferedImage Mat2BufferedImage(Mat m){
				// source: http://answers.opencv.org/question/10344/opencv-java-load-image-to-gui/
				// Fastest code
				// The output can be assigned either to a BufferedImage or to an Image

				int type = BufferedImage.TYPE_BYTE_GRAY;
				if ( m.channels() > 1 ) {
					type = BufferedImage.TYPE_3BYTE_BGR;
				}
				int bufferSize = m.channels()*m.cols()*m.rows();
				byte [] b = new byte[bufferSize];
				m.get(0,0,b); // get all the pixels
				BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
				final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
				System.arraycopy(b, 0, targetPixels, 0, b.length);  
				return image;

			}
			public void displayImage(Image img2)
			{   
				//BufferedImage img=ImageIO.read(new File("/HelloOpenCV/lena.png"));
				icon=new ImageIcon(img2);


				frame.setLayout(new FlowLayout());        
				frame.setSize(img2.getWidth(null)+50, img2.getHeight(null)+50);     

				lbl.setIcon(icon);
				frame.add(lbl);
				frame.setVisible(true);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			}
		}.start();
	
	
		System.out.println("good");
		try{
			table = NetworkTable.getTable("SmartDashboard");

		}
		catch(Exception e){

		}
		//		main loop of the program
	//	while(shouldRun){
			try {
				//				opens up the camera stream and tries to load it
				videoCapture = new VideoCapture();
				//				replaces the ##.## with your team number
				videoCapture.open(0);
				videoCapture.set(CAP_PROP_FRAME_WIDTH, 720);
				videoCapture.set(CAP_PROP_FRAME_HEIGHT, 405);
				videoCapture.set(CAP_PROP_EXPOSURE, -11);
				videoCapture.set(CAP_PROP_SATURATION, 2);
				//videoCapture.set(37, 1.0);
				//				Example
				//				cap.open("http://10.30.19.11/mjpg/video.mjpg");
				//				wait until it is opened
				while(!videoCapture.isOpened()){}
				//				time to actually process the acquired images
			/*	
				new Thread(){
					public void run(){
						while (true){
							videoCapture.read(matOriginal);
						}
					}
				}.start();
			*/	
				new MJpegHandler("Super_Dank_Vision",30,5805);
				processImage();
			} catch (Exception e) {
				e.printStackTrace();
	//			break;
			}
	//	}
		//		make sure the java process quits when the loop finishes
		 
		System.exit(0);

		/**
		 * 
		 * reads an image from a live image capture and outputs information to the SmartDashboard or a file
		 */


	}
	public static void processImage() throws InterruptedException{
		ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		double x,y,targetX,targetY,distance,azimuth;
		//		frame counter
		int FrameCount = 0;
		long before = System.currentTimeMillis();
		//		only run for the specified time

		while(true){
			time = System.currentTimeMillis();

			try{
				if (time-lastTime >= -1){
					fps = (1.0/((double)time - (double)lastTime)) * 1000;
					System.out.println(fps);
					videoCapture.read(matOriginal);
					Imgproc.cvtColor(matOriginal,matGray,Imgproc.COLOR_BGR2GRAY);		
					//Imgproc.medianBlur(matGray, matGray, 1);
					Imgproc.GaussianBlur(matGray, matGray, new Size(3,3), 0, 0);
					contours.clear();
					//			capture from the axis camera
					//			captures from a static file for testing
					//			matOriginal = Imgcodecs.imread("someFile.png");
					//Imgproc.cvtColor(matOriginal,matHSV,Imgproc.COLOR_BGR2HSV);			

					Imgproc.threshold(matGray, matThresh, thresh, 255, Imgproc.THRESH_BINARY);
					//	Core.inRange(matOriginal, LOWER_BOUNDS, UPPER_BOUNDS, matThresh);
					Imgproc.findContours(matThresh, contours, matHeirarchy, Imgproc.RETR_EXTERNAL, 
							Imgproc.CHAIN_APPROX_SIMPLE);
					//			make sure the contours that are detected are at least 20x20 
					//			pixels with an area of 400 and an aspect ration greater then 1
//					for (Iterator<MatOfPoint> iterator = contours.iterator(); iterator.hasNext();) {
//						MatOfPoint matOfPoint = (MatOfPoint) iterator.next();
//						//System.out.println(matOfPoint.size());
//						Rect rec = Imgproc.boundingRect(matOfPoint);
//						//System.out.printf("Height: %d   Width: %d\n",rec.height,rec.width);
//						if(rec.height < 20 || rec.width < 20){
//							iterator.remove();
//							continue;
//						}
//						float aspect = (float)rec.width/(float)rec.height;
//						if(aspect < 1.0){
//							iterator.remove();
//							continue;
//						}
//						double perimeter = Imgproc.arcLength(new org.opencv.core.MatOfPoint2f (matOfPoint.toArray()), true);
//						double expectedPerimeter = 4*rec.height + 2*rec.width;
//						if(perimeter > 1.5 * expectedPerimeter || perimeter < .5 * expectedPerimeter){
//							iterator.remove();
//							continue;
//						}
//						if(Imgproc.isContourConvex(matOfPoint)){
//							iterator.remove();
//							continue;
//						}
					//}
					//for(MatOfPoint mop : contours){
					//	Rect rec = Imgproc.boundingRect(mop);
						//System.out.println((float)rec.width/(float)rec.height);
//						Imgproc.rectangle(matOriginal, rec.br(), rec.tl(), BLACK);
//					}
					for (Iterator<MatOfPoint> iterator = contours.iterator(); iterator.hasNext();) {
						MatOfPoint matOfPoint = (MatOfPoint) iterator.next();
						MatOfPoint2f matOfPoint2f = new MatOfPoint2f(); matOfPoint.convertTo(matOfPoint2f, CvType.CV_32FC2);
						//System.out.println(matOfPoint.size());
						Rect rec = Imgproc.boundingRect(matOfPoint);
						//System.out.printf("Height: %d   Width: %d\n",rec.height,rec.width);
						if(rec.height < 20 || rec.width < 20){
							iterator.remove();
							continue;
						}
/*						
						float aspect = (float)rec.width/(float)rec.height;
						if(aspect < 1.0){
							iterator.remove();
							continue;
						}
						*/
						double perimeter = Imgproc.arcLength(matOfPoint2f, true);
						double expectedPerimeter = 4*rec.height + 2*rec.width;
						if(perimeter > 1.5 * expectedPerimeter || perimeter < .66 * expectedPerimeter){
							iterator.remove();
							continue;
						}
						
						if(Imgproc.isContourConvex(matOfPoint)){
							iterator.remove();
							continue;
						}
						
						MatOfPoint approxPoly = new MatOfPoint();
						MatOfPoint2f approxPoly2f = new MatOfPoint2f();
						Imgproc.approxPolyDP(matOfPoint2f, approxPoly2f, Imgproc.arcLength(matOfPoint2f, true) * .02, true);
						approxPoly2f.convertTo(approxPoly, CvType.CV_32S);
						if(!(approxPoly.size().height >= 7 && approxPoly.size().height <= 9)){
							//System.out.println("Detected wrong size");
							System.out.println("Number of vertices detected: " + approxPoly.size().height);
							iterator.remove();
							continue;
						}
					//}
					//for(MatOfPoint mop : contours){
					//	Rect rec = Imgproc.boundingRect(mop);
						//System.out.println((float)rec.width/(float)rec.height);
						Imgproc.rectangle(matOriginal, rec.br(), rec.tl(), BLACK);
					}
					Imgproc.drawContours(matOriginal, contours, -1,YELLOW);
					//			if there is only 1 target, then we have found the target we want
					System.out.println(contours.size());
					if (contours.size() >= 1){
						distMin = 0;
						min = 400;
						for (int i = 0; i < contours.size(); i++){
							Rect rec = Imgproc.boundingRect(contours.get(i));
							//				"fun" math brought to you by miss daisy (team 341)!
							y = rec.br().y + rec.height / 2;
							y= -((2 * (y / matOriginal.height())) - 1);
							distance = (TOP_TARGET_HEIGHT - TOP_CAMERA_HEIGHT) / 
									Math.tan((y * VERTICAL_FOV / 2.0 + CAMERA_ANGLE) * Math.PI / 180);
							//				angle to target...would not rely on this
							targetX = rec.tl().x + rec.width / 2;
							targetX = (2 * (targetX / matOriginal.width())) - 1;
							azimuth = normalize360(targetX*HORIZONTAL_FOV /2.0 + 0);
							//				drawing info on target
							Point center = new Point(rec.br().x-rec.width / 2 - 15,rec.br().y - rec.height / 2);
							Point centerw = new Point(rec.br().x-rec.width / 2 - 15,rec.br().y - rec.height / 2 - 20);
							Imgproc.putText(matOriginal, ""+(int)distance, center, Core.FONT_HERSHEY_PLAIN, 1, BLACK);
							Imgproc.putText(matOriginal, ""+(int)azimuth, centerw, Core.FONT_HERSHEY_PLAIN, 1, BLACK);
							
							if (distance < 250 && (azimuth < 15 || azimuth > 345) && distance > 10){
								if ( azimuth < min ){
									min = azimuth;
									distMin = distance;
								}               
							}
						}
						
						try{
							table.putNumber("Azimuth", min);
							table.putNumber("Distance", distMin);
						}
						catch (Exception e){

						}


					}
					else{
						table.putNumber("Distance", 0.0);
						table.putNumber("Azimuth", 400.0);

					}

					if (FrameCount%100 == 0){
					Imgcodecs.imwrite("output"+ FrameCount+".png", matOriginal);
					}
					//Imgcodecs.imwrite("output.png", matOriginal);
					FrameCount++;
					lastTime = time;
					matOriginal.copyTo(matStream);
				}

				//Thread.sleep(66);

				//			output an image for debugging
			}
			//	shouldRun = false;	
		
		catch (Exception e){

		}
		}
	}
	


	/**
	 * @param angle a nonnormalized angle
	 */
	public static double normalize360(double angle){
		while(angle >= 360.0)
		{
			angle -= 360.0;
		}
		while(angle < 0.0)
		{
			angle += 360.0;
		}
		return angle;
	}
}
