import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.awt.Image;

import java.io.File;
import java.io.IOException;
import javax.swing.JLabel;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.border.LineBorder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Result extends JFrame {

	private Toolkit toolkit;
	private double allowedMeanDeltaE;
	private double allowedMaxDeltaE;
	private double allowedMaxDeltaL;
	private double allowedMaxDeltaC;
	private double meanDeltaE;
	private double maxDeltaE;
	private double allowedDeviationRGB;
	private double[] minGainModulation;
	private double[] maxGainModulation;
	
	public Result(Measure currentImage, String qualityData, String target, String 
			referenceImage) 
		throws XPathExpressionException, ParserConfigurationException, 
			SAXException, IOException {
	
		minGainModulation = new double[4];
		maxGainModulation = new double[4];
		maxDeltaE = 0;
		readImageQuality(qualityData);
		
		setTitle("Colorite: Results");
		setSize(1150, 600);

		//main = new JPanel();
        //getContentPane().add(main);
        setLayout(new BorderLayout(5,5));
        
		// Center on screen
	 	toolkit = getToolkit();
	 	Dimension size = toolkit.getScreenSize();
	 	setLocation((size.width - getWidth())/2, (size.height - getHeight())/2);
	 	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
     
	 	// NORTH
	 	JPanel p1 = new JPanel();
	 	p1.setLayout(new GridLayout(2, 2, 0, 0));
	 	if (currentImage.getColorSpace().equals("LAB")) {
	 		p1.add(new JLabel("Target: " + target + "  "));
	 		p1.add(new JLabel("Reference Image: " + referenceImage + "  "));
	 		p1.add(new JLabel("Image quality: " + qualityData + "  "));
	 		p1.add(new JLabel(""));
	 	}
	 	else {
		 	p1.add(new JLabel("Colorspace: RGB"));
		 	p1.add(new JLabel("Max deviation: " + allowedDeviationRGB));
		 }	
	 	add(p1, BorderLayout.NORTH);
	 	
	 	// WEST
	 	JPanel p2 = new JPanel();
	 	p2.setLayout(new GridLayout(currentImage.getNumPatches()+2, 5, 0, 0));
        p2.add(new JLabel("Patch"));
        p2.add(new JLabel("Reference value    "));
        p2.add(new JLabel("Measured value"));
        p2.add(new JLabel("Noise"));
        p2.add(new JLabel("Delta-E"));
        p2.add(new JLabel("Outcome"));
    
        for (int i = 0; i < currentImage.getNumPatches(); i++){
 
            p2.add(new JLabel((i+1) + ":"));
        
            // Reference color values
            String values = "(";
            for (int j = 0; j < 3; j++){ 
            	values = values + (double)Math.round(10*currentImage.getReferenceColorValue(i, j))/10;
            	if (j != 2)
            		values = values + ", ";
            }
            values = values + ")";
            p2.add(new JLabel(values));
            
            // Measured color values
            values = "(";
            for (int j = 0; j < 3; j++){ 
            	values = values + 
            		(double)Math.round(10*currentImage.getMeasuredColorValue(i, j))/10;;
            	if (j != 2)
            		values = values + ", ";
            }
            values = values + ")";
            p2.add(new JLabel(values));
            
            p2.add(new JLabel(Double.toString(currentImage.getStdDev(i))));
            
            // Print deltaE and compute pass/fail 
            if (currentImage.getColorSpace().equals("LAB")) {
            	p2.add(new JLabel(Double.toString(currentImage.getDeltaE(i))));
            	double res = currentImage.getDeltaE(i);
            	if (res > maxDeltaE)
            		maxDeltaE = res;
            	if (res <= allowedMaxDeltaE) {
            		JLabel pass = new JLabel("Pass");
            		pass.setForeground(Color.green);
            		p2.add(pass);
            	}
            	else {
            		JLabel fail = new JLabel("Fail");
            		fail.setForeground(Color.red);
            		p2.add(fail);
            	}
            }
            // Print deviation and compute pass/fail 
            else {
            	int maxDev = 0;
            	for (int j = 0; j < 3; j++){ 
            		if (currentImage.getDeviationRGB(i, j) > maxDev)
            			maxDev = currentImage.getDeviationRGB(i, j);
            	}
            	p2.add(new JLabel(Integer.toString(maxDev)));
            	if (maxDev <= allowedDeviationRGB) {
            		JLabel pass = new JLabel("Pass");
            		pass.setForeground(Color.green);
            		p2.add(pass);
            	}
            	else {
            		JLabel fail = new JLabel("Fail");
            		fail.setForeground(Color.red);
            		p2.add(fail);
            	}
            }
        }
        p2.add(new JLabel("Mean:"));
        p2.add(new JLabel(""));
        p2.add(new JLabel(""));
        p2.add(new JLabel(""));
        meanDeltaE = currentImage.getMeanDeltaE();
        p2.add(new JLabel(Double.toString(meanDeltaE)));
        if (meanDeltaE <= allowedMeanDeltaE){
    		JLabel pass = new JLabel("Pass");
    		pass.setForeground(Color.green);
    		p2.add(pass);
    	}
    	else {
    		JLabel fail = new JLabel("Fail");
    		fail.setForeground(Color.red);
    		p2.add(fail);
    	}
        	
        add(p2, BorderLayout.WEST);
       
        // CENTER
        JPanel p3 = new JPanel();
        p3.setLayout(new GridLayout((currentImage.getNumPatches()+2), 4, 0, 0));
        p3.add(new JLabel("Delta-L"));
        p3.add(new JLabel("Outcome"));
        p3.add(new JLabel("Delta-C"));
        p3.add(new JLabel("Outcome"));
        int count = 0;
        int limit = currentImage.getPatchesBeforeChange();
        // limit = 0 when we don't have any color patches -> more logical
        // description of the target 
        
        if (limit == 0)
        	limit = currentImage.getNumPatches();
        if (currentImage.getStartWithGrayscale() == 1) {
        	for (int i = 0; i < limit; i++) {
        		count+=4;
        		// Delta-L
        		double res =currentImage.getDeltaL(i);
        		p3.add(new JLabel(Double.toString(res)));
        		if (res <= allowedMaxDeltaL) {
            		JLabel pass = new JLabel("Pass");
            		pass.setForeground(Color.green);
            		p3.add(pass);
            	}
            	else {
            		JLabel fail = new JLabel("Fail");
            		fail.setForeground(Color.red);
            		p3.add(fail);
            	}
        		// Delta-C
        		res =currentImage.getDeltaC(i);
        		p3.add(new JLabel(Double.toString(res)));
        		if (res <= allowedMaxDeltaC) {
            		JLabel pass = new JLabel("Pass");
            		pass.setForeground(Color.green);
            		p3.add(pass);
            	}
            	else {
            		JLabel fail = new JLabel("Fail");
            		fail.setForeground(Color.red);
            		p3.add(fail);
            	}
        		
        	}
        }
        else {
        	// Delta-L
        	for (int i = limit; i < currentImage.getNumPatches(); i++) {
        		count+=4;
        		double res =currentImage.getDeltaL(i);
        		p3.add(new JLabel(Double.toString(res)));
        		if (res <= allowedMaxDeltaC) {
            		JLabel pass = new JLabel("Pass");
            		pass.setForeground(Color.green);
            		p3.add(pass);
            	}
            	else {
            		JLabel fail = new JLabel("Fail");
            		fail.setForeground(Color.red);
            		p3.add(fail);
            	}
        		// Delta-C
        		res =currentImage.getDeltaC(i);
        		p3.add(new JLabel(Double.toString(res)));
        		if (res <= allowedMaxDeltaC) {
            		JLabel pass = new JLabel("Pass");
            		pass.setForeground(Color.green);
            		p3.add(pass);
            	}
            	else {
            		JLabel fail = new JLabel("Fail");
            		fail.setForeground(Color.red);
            		p3.add(fail);
            	}
        	}
        }
        count = count/4;
        for (int i = count+1; i<currentImage.getNumPatches()+2; i++ ) {
        	p3.add(new JLabel(""));
        	p3.add(new JLabel(""));
        	p3.add(new JLabel(""));
        	p3.add(new JLabel(""));
        }
        add(p3, BorderLayout.CENTER);
        
        // EAST        
        JPanel p4 = new JPanel();
        p4.setLayout(new GridLayout((currentImage.getNumPatches()+2), 3, 0, 0));
        p4.add(new JLabel("Gain Modulation  "));
        p4.add(new JLabel("Value"));
        p4.add(new JLabel("Outcome"));
        count = 0;
        for (int i = 0; i<4; i++ ) {
        	if (currentImage.getGMName(i) != null) {
        		count+=3;
        		p4.add(new JLabel(currentImage.getGMName(i) + ":"));
        		double res = currentImage.getGM(i);
        		p4.add(new JLabel(Double.toString(res)));
        		if (res >= minGainModulation[i] && res <= maxGainModulation[i]) {
            		JLabel pass = new JLabel("Pass");
            		pass.setForeground(Color.green);
            		p4.add(pass);
            	}
            	else {
            		JLabel fail = new JLabel("Fail");
            		fail.setForeground(Color.red);
            		p4.add(fail);
            	}	
        	}   
        }

        for (int i = count; i<currentImage.getNumPatches()+2; i++ ) {
        	p4.add(new JLabel(""));
        	p4.add(new JLabel(""));
        	p4.add(new JLabel(""));
        }
        add(p4, BorderLayout.EAST);
        
        // SOUTH
        JPanel p5 = new JPanel();
	 	p5.setLayout(new FlowLayout());
        p5.add(new JLabel("Image resolution: "));
        p5.add(new JLabel(Double.toString(currentImage.getResolution()) + " ppi"));
        add(p5, BorderLayout.SOUTH);
	}
		
	// Read image quality
	public void readImageQuality(String qualityFilename) throws ParserConfigurationException, 
		SAXException, IOException, XPathExpressionException {

	    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
	    domFactory.setNamespaceAware(true); // never forget this!
	    
	    DocumentBuilder builder = domFactory.newDocumentBuilder();
	    Document doc = builder.parse(qualityFilename);

	    XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
	    
	    // Read Gain Modulation
	    // Get min-values
	    XPathExpression expr 
	     = xpath.compile("//gainModulation/*/min/text()");
	    Object result = expr.evaluate(doc, XPathConstants.NODESET);
	    NodeList nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	        minGainModulation[i] = Double.valueOf(nodes.item(i).getNodeValue());
	    }
	    
	    // Get max-values
	    expr = xpath.compile("//gainModulation/*/max/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	        maxGainModulation[i] = Double.valueOf(nodes.item(i).getNodeValue());
	    }
	    
	    // Delta-E
	    // Min
	    expr = xpath.compile("//colorAccuracy/delta-e/mean/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    allowedMeanDeltaE = Integer.valueOf(nodes.item(0).getNodeValue());
	    
	    // Delta-E
	    // Max
	    expr = xpath.compile("//colorAccuracy/delta-e/max/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    allowedMaxDeltaE = Integer.valueOf(nodes.item(0).getNodeValue());
	  
	    // Delta-L
	    expr = xpath.compile("//delta-L/max/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    allowedMaxDeltaL = Integer.valueOf(nodes.item(0).getNodeValue());
	   
	    // Delta-C
	    expr = xpath.compile("//delta-c/max/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    allowedMaxDeltaC = Integer.valueOf(nodes.item(0).getNodeValue());
	    
	}
	
}
