import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MJpegHandler implements HttpHandler{
		
		private BufferedImage bufferedImage;
		private int desiredFrameRate;
		
		public MJpegHandler(String appLocation, int desiredFrameRate, int port){
			super();
			this.desiredFrameRate = desiredFrameRate;
			com.sun.net.httpserver.HttpServer server;
			try {
				server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
				server.createContext("/" + appLocation, this);
	    		server.setExecutor(null);
	    		server.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		
		public void handle(HttpExchange connection) throws IOException {
	        byte[] data;

	        //System.out.println("Connect...");

	        String boundary = "boundarydontcross";

	        Headers responseHeaders = connection.getResponseHeaders();
	        responseHeaders.add("Cache-Control", "no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0");
	        responseHeaders.add("Connection", "close");
	        responseHeaders.add("Content-Type", String.format("multipart/x-mixed-replace; boundary=--%s", boundary));       
	        responseHeaders.add("Expires", "Mon, 3 Jan 2000 12:34:56 GMT");
	        responseHeaders.add("Pragma", "no-cache");
	        connection.sendResponseHeaders(200, 0);
	        OutputStream responseBody = connection.getResponseBody();
	        responseBody.flush();


	        while (!connection.getRequestHeaders().isEmpty()) {
	        	
	        	System.out.println("streaming");

	        	bufferedImage = Mat2BufferedImage(TowerTracker.matStream);
	            ByteArrayOutputStream os = new ByteArrayOutputStream();
	            ImageIO.write(bufferedImage, "jpeg", os);
	            data = os.toByteArray();
	            os.close();


	            responseBody.write(("--" + boundary + "\r\n"
	                    + "Content-type: image/jpeg\r\n"
	                    + "Content-Length: "
	                    + data.length
	                    + "\r\n\r\n").getBytes());

	            responseBody.write(data);
	            responseBody.flush();
	            //responseBody.close();
	            
	            try {
					Thread.sleep((int)(1.0/(float)desiredFrameRate));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
		}

	}