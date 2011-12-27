	import java.awt.Graphics;
	import java.awt.GridLayout;
	import java.awt.image.BufferedImage;
	import java.io.File;
	import java.io.IOException;
	import javax.imageio.ImageIO;
	import javax.swing.JFrame;
	import javax.swing.JPanel;
	
	// Simple class to show the output image

	public class ShowImage extends JFrame {

		private JPanel panel;
		private BufferedImage image;
		
		public ShowImage(String imageName) {
		
			setTitle("Colorite: Reference image");
		
			panel = new JPanel();
	        getContentPane().add(panel);
	        panel.setLayout(new GridLayout(1,1));
	        
			// Center on screen
		 //	toolkit = getToolkit();
		 //	Dimension size = toolkit.getScreenSize();
		 	setLocation(10, 10);
		 	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		 	
		 	try {
		 		File input = new File(imageName);
		 		image = ImageIO.read(input);
		 		setSize(image.getWidth(), image.getHeight());
		 	} 
		 	catch (IOException ie) {
		 		System.out.println("Error:"+ie.getMessage());
		 	}
				
		}
		
		public void paint(Graphics g) {
		    g.drawImage( image, 0, 0, null);
		  }
		
	}
		 