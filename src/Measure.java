/*
 * 
 * Ideas for improvement:
 * Divide color values into separate xml-tags for gray scale and color. 
 * Note: need to be able to specify which patches are color and which are gray
 * 
 */

/* Note: Measure only works with color images. For gray images, an exception is 
 * currently thrown when the color space is determined. We do this since some files,
 * e.g. icc-profiles, are accepted by ImageMagick.
*/ 

import java.io.*;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

public class Measure {

	private int numPatches;
	private int startWithGrayscale;
	private int patchesBeforeChange;
	private int[][] patchLocations;
	private int[][] targetLocation;
	private double[] targetSizeOriginal;
	private double[][] referenceColorValuesLAB;
	private int[][] referenceColorValuesRGB;
	private double[][] measuredColorValues;
	private double[] deltaE;
	private double[] deltaC;
	private double[] deltaL;
	private double[] stdDev;
	private double[] gainModulation;
	private String[] gainModulationName;
	private int[] gainModulationPatches;
	private int[][] deviationRGB;
	private double targetLength;
	private double resolution;
	private double maxDeltaE;
	private double maxDeltaC;
	private double maxDeltaL;
	private double meanDeltaL;
	private double meanDeltaE;
	private double meanDeltaC;
	private double meanDeviationRGB;
	private int maxDeviationRGB;
	private String colorSpace;
	private String targetName;
	
	// TODO Write fail/pass to xml-file?
	
	public Measure(String imageName, String targetName, String imageDataTemplateFilename, 
			String imageDataFilename, String targetDataFilename, String outputImage, 
			String find, String imageMagick) 
		throws XPathExpressionException, ParserConfigurationException, 
		SAXException, IOException, IMException, FindException{
		readNumPatches(targetDataFilename);
		patchLocations = new int[numPatches][2];
		referenceColorValuesLAB = new double [numPatches][3];
		referenceColorValuesRGB = new int [numPatches][3];
		deviationRGB = new int [numPatches][3];
		gainModulation = new double[4];
		gainModulationName = new String[4];
		gainModulationPatches = new int[8];
		measuredColorValues = new double[numPatches][3];
		targetLocation = new int[4][2];
		targetSizeOriginal = new double[2];
		deltaE = new double[numPatches];
		deltaL = new double[numPatches];
		deltaC = new double[numPatches];
		stdDev = new double[numPatches];
		readTargetData(targetDataFilename);
		computePatchLocation(imageName, imageDataTemplateFilename, imageDataFilename, 
				targetName, targetDataFilename, outputImage, find);
		readImageData(imageDataFilename);
		computeResolution();
		checkColorSpace(imageName, imageMagick);	
	}

	// Compute deltaE for all patches
	public void computeDeltaE() {
		
		double sum = 0; double max = 0;
		double l, a, b, temp;
		int usedPatches = 0;
		
		for (int i = 0; i < numPatches; i++) {
			l = Math.pow(referenceColorValuesLAB[i][0] - 
					measuredColorValues[i][0], 2);
			a = Math.pow(referenceColorValuesLAB[i][1] - 
					measuredColorValues[i][1], 2);
			b = Math.pow(referenceColorValuesLAB[i][2] - 
					measuredColorValues[i][2], 2);
			temp = Math.sqrt(l+a+b);
			deltaE[i] = (double)Math.round(temp*100)/100;
			// We're only interested in the color patches for the aggregate
			if ((startWithGrayscale == 1 && i >= patchesBeforeChange) ||
					(startWithGrayscale == 0 && i < patchesBeforeChange)) {
				sum += temp;
				usedPatches++;
				if (temp > max)
					max = temp;
			}		
		}
		
		// Use two decimal places
		maxDeltaE = (double)Math.round(max*100)/100;
		meanDeltaE = (double)Math.round((100*sum/(double)usedPatches))/100;
		//System.out.println("meanDeltaE " + meanDeltaE);
	}

	// Compute deltaC for all grayscale patches
	public void computeDeltaC() {
		double sum = 0; 
		double usedPatches = 0;
		double max = 0;
		double a, b,temp;
		if (startWithGrayscale == 1) {
			// patchesBeforeChange = 0 when we don't have any color patches 
			// -> more logical description of the target 
			if (patchesBeforeChange == 0)
				patchesBeforeChange = numPatches;
			for (int i = 0; i < patchesBeforeChange; i++) {
				a = Math.pow(referenceColorValuesLAB[i][1] - 
						measuredColorValues[i][1], 2);
				b = Math.pow(referenceColorValuesLAB[i][2] - 
						measuredColorValues[i][2], 2);
				temp = Math.sqrt(a+b);
				deltaC[i] = (double)Math.round(temp*100)/100;
				if (temp > max)
					max = temp;
				sum += temp;
				usedPatches++;
				}
		}
		else {
			for (int i = patchesBeforeChange; i < numPatches; i++) {
				a = Math.pow(referenceColorValuesLAB[i][1] - 
						measuredColorValues[i][1], 2);
				b = Math.pow(referenceColorValuesLAB[i][2] - 
						measuredColorValues[i][2], 2);
			
				temp = Math.sqrt(a+b);
				deltaC[i] = (double)Math.round(temp*100)/100;
				if (temp > max)
					max = temp;
				sum += temp;
				usedPatches++;
			}
		}
		
		// Use two decimal places
		maxDeltaC = (double)Math.round(max*100)/100;
		meanDeltaC = (double)Math.round(100*sum/usedPatches)/100;
	}
	
	// Compute deltaL for all grayscale patches
	public void computeDeltaL() {
		double max = 0; 
		double sum = 0; 
		double usedPatches = 0;
		double l, temp;
		if (startWithGrayscale == 1) {
			// patchesBeforeChange = 0 when we don't have any color patches 
			// -> more logical description of the target 
			if (patchesBeforeChange == 0)
				patchesBeforeChange = numPatches;
			for (int i = 0; i < patchesBeforeChange; i++) {
				l = Math.pow(referenceColorValuesLAB[i][0] - 
					measuredColorValues[i][0], 2);
				temp = Math.sqrt(l);
				deltaL[i] = (double)Math.round(temp*100)/100;
				if (temp > max)
					max = temp;
				sum += temp;
				usedPatches++;
				}
		}
		else {
			for (int i = patchesBeforeChange; i < numPatches; i++) {
				l = Math.pow(referenceColorValuesLAB[i][0] - 
					measuredColorValues[i][0], 2);
			
				temp = Math.sqrt(l);
				deltaL[i] = (double)Math.round(temp*100)/100;
				if (temp > max)
					max = temp;
				sum += temp;
				usedPatches++;
			}
		}
		
		// Use two decimal places
		maxDeltaL = (double)Math.round(max*100)/100;
		meanDeltaL = (double)Math.round(100*sum/usedPatches)/100;
	}
	
	// Compute the gain modulation
	// Remember that the entries in gainModulationPatches doesn't correspond
	// to the index, you need to subtract one to get the correct value.
	public void computeGainModulation() {
		// L*95-L*90
		boolean highlights = false;  
		if (gainModulationPatches[0] != 0 && 
				gainModulationPatches[1] != 0) {
			/*double a = Math.pow((measuredColorValues[gainModulationPatches[0]-1][0] - 
				measuredColorValues[gainModulationPatches[1]-1][0]),2);
			double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[0]-1][0] - 
				referenceColorValuesLAB[gainModulationPatches[1]-1][0],2);
			gainModulation[0] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
			double a = measuredColorValues[gainModulationPatches[0]-1][0] - 
					measuredColorValues[gainModulationPatches[1]-1][0];
			double b = referenceColorValuesLAB[gainModulationPatches[0]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[1]-1][0];
			gainModulation[0] = (double)Math.round(100*a/b)/100;
			highlights = true;
			gainModulationName[0] = "L*95-L*90";
		}
		// L*90-L*85
		if (gainModulationPatches[1] != 0 && 
				gainModulationPatches[2] != 0) {
			/*double a = Math.pow((measuredColorValues[gainModulationPatches[1]-1][0] - 
				measuredColorValues[gainModulationPatches[2]-1][0]),2);
			double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[1]-1][0] - 
				referenceColorValuesLAB[gainModulationPatches[2]-1][0],2);
			gainModulation[1] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
			double a = measuredColorValues[gainModulationPatches[1]-1][0] - 
					measuredColorValues[gainModulationPatches[2]-1][0];
			double b = referenceColorValuesLAB[gainModulationPatches[1]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[2]-1][0];
				gainModulation[1] = (double)Math.round(100*a/b)/100;
			highlights = true;
			gainModulationName[1] = "L*90-L*85";
		}
		// The highlights are important so we'll try to compute the gain modulation
		// for three more combinations if L90-L90 and L90-L85 hasn't been computed.
		if (highlights == false) {
			// L*95-L*85
			if (gainModulationPatches[0] != 0 && 
					gainModulationPatches[2] != 0) {
				/*double a = Math.pow((measuredColorValues[gainModulationPatches[0]-1][0] - 
					measuredColorValues[gainModulationPatches[2]-1][0]),2);
				double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[0]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[2]-1][0],2);
				gainModulation[0] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
				double a = measuredColorValues[gainModulationPatches[0]-1][0] - 
						measuredColorValues[gainModulationPatches[2]-1][0];
				double b = referenceColorValuesLAB[gainModulationPatches[0]-1][0] - 
						referenceColorValuesLAB[gainModulationPatches[2]-1][0];
				gainModulation[0] = (double)Math.round(100*a/b)/100;
				gainModulationName[0] = "L*95-L*85";
			}
			// L*95-L*80
			else if (gainModulationPatches[0] != 0 && 
					gainModulationPatches[3] != 0) {
				/*double a = Math.pow((measuredColorValues[gainModulationPatches[0]-1][0] - 
					measuredColorValues[gainModulationPatches[3]-1][0]),2);
				double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[0]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[3]-1][0],2);
				gainModulation[0] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
				double a = measuredColorValues[gainModulationPatches[0]-1][0] - 
						measuredColorValues[gainModulationPatches[3]-1][0];
				double b = referenceColorValuesLAB[gainModulationPatches[0]-1][0] - 
						referenceColorValuesLAB[gainModulationPatches[3]-1][0];
				gainModulation[0] = (double)Math.round(100*a/b)/100;
				gainModulationName[0] = "L*95-L*80";
			}
			// L*90-L*80
			else if (gainModulationPatches[1] != 0 && 
					gainModulationPatches[3] != 0) {
				/*double a = Math.pow((measuredColorValues[gainModulationPatches[1]-1][0] - 
					measuredColorValues[gainModulationPatches[3]-1][0]),2);
				double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[1]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[3]-1][0],2);
				gainModulation[0] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
				double a = measuredColorValues[gainModulationPatches[1]-1][0] - 
						measuredColorValues[gainModulationPatches[3]-1][0];
				double b = referenceColorValuesLAB[gainModulationPatches[1]-1][0] - 
						referenceColorValuesLAB[gainModulationPatches[3]-1][0];
				gainModulation[0] = (double)Math.round(100*a/b)/100;
				gainModulationName[0] = "L*90-L*80";
			}
			
		}
		// L*85-L*80 to L*25-L*20
		// L*85-L*25
		if (gainModulationPatches[2] != 0 && 
				gainModulationPatches[4] != 0) {
			/*double a = Math.pow((measuredColorValues[gainModulationPatches[2]-1][0] - 
				measuredColorValues[gainModulationPatches[4]-1][0]),2);
			double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[2]-1][0] - 
				referenceColorValuesLAB[gainModulationPatches[4]-1][0],2);
			gainModulation[2] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
			double a = measuredColorValues[gainModulationPatches[2]-1][0] - 
					measuredColorValues[gainModulationPatches[4]-1][0];
			double b = referenceColorValuesLAB[gainModulationPatches[2]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[4]-1][0];
			gainModulation[2] = (double)Math.round(100*a/b)/100;
			gainModulationName[2] = "L*85-L*25";
		}
		// L*85-L*20
		else if (gainModulationPatches[2] != 0 && 
				gainModulationPatches[5] != 0) {
			/*double a = Math.pow((measuredColorValues[gainModulationPatches[2]-1][0] - 
				measuredColorValues[gainModulationPatches[5]-1][0]),2);
			double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[2]-1][0] - 
				referenceColorValuesLAB[gainModulationPatches[5]-1][0],2);
			gainModulation[2] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
			double a = measuredColorValues[gainModulationPatches[2]-1][0] - 
					measuredColorValues[gainModulationPatches[5]-1][0];
			double b = referenceColorValuesLAB[gainModulationPatches[2]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[5]-1][0];
			gainModulation[2] = (double)Math.round(100*a/b)/100;
			gainModulationName[2] = "L*85-L*20";
		}
		// L*80-L*25
		else if (gainModulationPatches[3] != 0 && 
				gainModulationPatches[4] != 0) {
			/*double a = Math.pow((measuredColorValues[gainModulationPatches[3]-1][0] - 
				measuredColorValues[gainModulationPatches[4]-1][0]),2);
			double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[3]-1][0] - 
				referenceColorValuesLAB[gainModulationPatches[4]-1][0],2);
			gainModulation[2] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
			double a = measuredColorValues[gainModulationPatches[3]-1][0] - 
					measuredColorValues[gainModulationPatches[4]-1][0];
				double b = referenceColorValuesLAB[gainModulationPatches[3]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[4]-1][0];
				gainModulation[2] = (double)Math.round(100*a/b)/100;
			gainModulationName[2] = "L*80-L*25";
		}
		// L*80-L*20
		else if (gainModulationPatches[3] != 0 && 
				gainModulationPatches[5] != 0) {
			/*double a = Math.pow((measuredColorValues[gainModulationPatches[3]-1][0] - 
				measuredColorValues[gainModulationPatches[5]-1][0]),2);
			double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[3]-1][0] - 
				referenceColorValuesLAB[gainModulationPatches[5]-1][0],2);
			gainModulation[2] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
			double a = measuredColorValues[gainModulationPatches[3]-1][0] - 
					measuredColorValues[gainModulationPatches[5]-1][0];
				double b = referenceColorValuesLAB[gainModulationPatches[3]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[5]-1][0];
				gainModulation[2] = (double)Math.round(100*a/b)/100;
			gainModulationName[2] = "L*80-L*20";
		}
		// L*85-L*80 to L*10-L*5
		// L*85-L*10
		if (gainModulationPatches[2] != 0 && 
				gainModulationPatches[6] != 0) {
			/*double a = Math.pow((measuredColorValues[gainModulationPatches[2]-1][0] - 
				measuredColorValues[gainModulationPatches[6]-1][0]),2);
			double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[2]-1][0] - 
				referenceColorValuesLAB[gainModulationPatches[6]-1][0],2);
			gainModulation[3] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
			double a = measuredColorValues[gainModulationPatches[2]-1][0] - 
					measuredColorValues[gainModulationPatches[6]-1][0];
				double b = referenceColorValuesLAB[gainModulationPatches[2]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[6]-1][0];
				gainModulation[3] = (double)Math.round(100*a/b)/100;
			gainModulationName[3] = "L*85-L*10";
		}
		// L*85-L*5
		else if (gainModulationPatches[2] != 0 && 
				gainModulationPatches[7] != 0) {
			/*double a = Math.pow((measuredColorValues[gainModulationPatches[2]-1][0] - 
				measuredColorValues[gainModulationPatches[7]-1][0]),2);
			double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[2]-1][0] - 
				referenceColorValuesLAB[gainModulationPatches[7]-1][0],2);
			gainModulation[3] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
			double a = measuredColorValues[gainModulationPatches[2]-1][0] - 
					measuredColorValues[gainModulationPatches[7]-1][0];
				double b = referenceColorValuesLAB[gainModulationPatches[2]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[7]-1][0];
				gainModulation[3] = (double)Math.round(100*a/b)/100;
			gainModulationName[3] = "L*85-L*5";
		}
		//L*80-L*10
		else if (gainModulationPatches[3] != 0 && 
				gainModulationPatches[6] != 0) {
			/*double a = Math.pow((measuredColorValues[gainModulationPatches[3]-1][0] - 
				measuredColorValues[gainModulationPatches[6]-1][0]),2);
			double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[3]-1][0] - 
				referenceColorValuesLAB[gainModulationPatches[6]-1][0],2);
			gainModulation[3] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
			double a = measuredColorValues[gainModulationPatches[3]-1][0] - 
					measuredColorValues[gainModulationPatches[6]-1][0];
				double b = referenceColorValuesLAB[gainModulationPatches[3]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[6]-1][0];
				gainModulation[3] = (double)Math.round(100*a/b)/100;
			gainModulationName[3] = "L*80-L*10";
		}
		//L*80-L*5
		else if (gainModulationPatches[3] != 0 && 
				gainModulationPatches[7] != 0) {
			/*double a = Math.pow((measuredColorValues[gainModulationPatches[3]-1][0] - 
				measuredColorValues[gainModulationPatches[7]-1][0]),2);
			double b = Math.pow(referenceColorValuesLAB[gainModulationPatches[3]-1][0] - 
				referenceColorValuesLAB[gainModulationPatches[7]-1][0],2);
			gainModulation[3] = (double)Math.round(100*Math.sqrt(a)/Math.sqrt(b))/100;*/
			double a = measuredColorValues[gainModulationPatches[3]-1][0] - 
					measuredColorValues[gainModulationPatches[7]-1][0];
				double b = referenceColorValuesLAB[gainModulationPatches[3]-1][0] - 
					referenceColorValuesLAB[gainModulationPatches[7]-1][0];
				gainModulation[3] = (double)Math.round(100*a/b)/100;
			gainModulationName[3] = "L*80-L*5";
		}
		System.out.println("Gain modulation computed");
	}
	
	// Compute RGB deviations
	public void computeDeviationRGB() {
	
		maxDeviationRGB = 0;
		double sum = 0; 
		for (int i = 0; i < numPatches; i++) {
			// COmpute mean deviation
			deviationRGB[i][0] = (int) Math.round(Math.abs(referenceColorValuesRGB[i][0] - 
					measuredColorValues[i][0]));
			deviationRGB[i][1] = (int) Math.round(Math.abs(referenceColorValuesRGB[i][1] - 
					measuredColorValues[i][1]));
			deviationRGB[i][2] = (int) Math.round(Math.abs(referenceColorValuesRGB[i][2] - 
					measuredColorValues[i][2]));
			sum += deviationRGB[i][0] + deviationRGB[i][1] + 
				deviationRGB[i][2];
			// Use two decimal places
			meanDeviationRGB = (double)Math.round((sum/((double)numPatches*3))*100)/100;
			
			// Compute max deviation
			if (deviationRGB[i][0] > maxDeviationRGB)
				maxDeviationRGB = deviationRGB[i][0];
			if (deviationRGB[i][1] > maxDeviationRGB)
				maxDeviationRGB = deviationRGB[i][1];
			if (deviationRGB[i][2] > maxDeviationRGB)
				maxDeviationRGB = deviationRGB[i][2];
		}		
	}
	
	// Check the color space of the image
	public void checkColorSpace(String imageName, String imageMagick) 
		throws IMException{	
		// Check the type of color space
		String output = new String();
		try {
			Process p = Runtime.getRuntime().exec
			(imageMagick + "identify -format " + "'%[colorspace]' " + imageName);
			BufferedReader input =
		        new BufferedReader
		          (new InputStreamReader(p.getInputStream()));
		 
			// Wait until ImageMagick finishes
			p.waitFor();
			output = input.readLine();
			input.close();
		}
		catch (Exception err) {
			err.printStackTrace();
		}
		
		// If ImageMagick returns null, there's an error with the file
		if (output == null) {
			throw new IMException();
		}
		
		if (output.equals("'Lab'")) {
			System.out.println("Color space: LAB");
			colorSpace = "LAB";
		}
		
		else if (output.equals("'RGB'")) {
			System.out.println("Color space: RGB");
			colorSpace = "RGB";
		}
		else
			throw new IMException();
	}
	
	// Measure the color values in the image. Also computes the noise since we don't
	// retain the individual color values in a sample
	// Include check on colorspace!
	// Include step to modify color values to a more common interval!
	public void measurePatches(int stencil, String filename, String imageMagick,
			String workingDir) throws IMException {
		
		// Delete old file, we use the existence of the file to 
		// check if ImageMagick worked correctly
		// Previously just a slash without color ?!?
		File color = new File(workingDir + "/color.txt");
		if (color.exists()){
			color.delete();
		}
		
		for (int i = 0; i < numPatches; i++) {
			// Run Imagemagick, the output is stored in color.txt. 
			// It's impossible to get the result to stdout.
			try {
				Process p = Runtime.getRuntime().exec
				(imageMagick + "convert -crop " + stencil +"x" + stencil + "+" + 
						(patchLocations[i][0] + stencil/2) + "+" + 
						(patchLocations[i][1] + stencil/2) + " " + filename + 
						" +repage " + workingDir + "/color.txt");
				// Wait until ImageMagick finishes
				p.waitFor();
			}
			catch (Exception err) {
				err.printStackTrace();
			}
			
			// If color.txt doesn't exist, throw an exception as this indicates
			// errors in the file. This is not a pretty solution but I don't 
			// know how to check ImageMagick
			if (!color.exists()) {
				throw new IMException();
			}
			
			// Read the color values  from the output file and 
			// compute the average value.
			// Delimiters to parse the output
			String delimiters = "[ .,?!£\\(\\)]+";
			double colors[] = {0,0,0};
			double stdValues[][] = new double[3][stencil*stencil];
		    try {
		      BufferedReader input =  new BufferedReader(
		    		  new FileReader(workingDir + "/color.txt"));
		      try {
		        String line = null; 
		        int j = 0;
		        // Read each line and parse the data
		        while (( line = input.readLine()) != null){
		        	String[] tokens = null;
		        	tokens = line.split(delimiters);
		        	// First line in the file starts with "# ImageMagick pixel..."
		        	if (!tokens[0].equals("#")) {
		        		// The first two tokens from each line are coordinates
		        		// Need to convert the values to Photoshop standard
		        		if (colorSpace.equals("LAB")) {
		        			colors[0] += (100.0/255.0)*Double.valueOf(tokens[2]);
//		        			stdValues[0][j] = (100.0/255.0)*Double.valueOf(tokens[2]);
		        			stdValues[0][j] = Double.valueOf(tokens[2]);

		        		}
		        		else {
		        			colors[0] += Double.valueOf(tokens[2]);
		        			stdValues[0][j] = Double.valueOf(tokens[2]);
		        		}
		        		if (Double.valueOf(tokens[3]) < 128 || colorSpace.equals("RGB")) {
		        			colors[1] += Double.valueOf(tokens[3]);
		        			stdValues[1][j] = Double.valueOf(tokens[3]);
		        		}
		        		else {
		        			colors[1] += Double.valueOf(tokens[3])-256;
		        			stdValues[1][j] = Double.valueOf(tokens[3])-256;
		        		}
		        		if (Double.valueOf(tokens[4]) < 128 || colorSpace.equals("RGB")) {
		        			colors[2] += Double.valueOf(tokens[4]);
		        			stdValues[2][j] = Double.valueOf(tokens[4]);
		        		}
		        		else {
		        			colors[2] += Double.valueOf(tokens[4])-256;
		        			stdValues[2][j] = Double.valueOf(tokens[4])-256;
		        		}
		        		j++;
		        	}	        	
		        }
		      }
		      finally {
		        input.close();
		      }
		    }
		    catch (IOException ex){
		      ex.printStackTrace();
		    }
		    catch (ArrayIndexOutOfBoundsException ex){
			      ex.printStackTrace();
			}
		    
		    // Compute average color values
		    for (int j = 0; j < 3; j++) {
		    	measuredColorValues[i][j] = (double)Math.round(10*colors[j]/(stencil*stencil))/10;
		    }
		    
		    // Measure standard deviation
		    // Note: only on L-channel! Previously, we used all channel,
		    // hence the now truncated loop.
		    double sum = 0;
		    for (int j = 0; j < 1; j++) {
		    	 for (int k = 0; k < (stencil*stencil); k++) {
		    		 // Math.round for debugging!
		    		 sum += Math.pow(measuredColorValues[i][j]/(100.0/255.0) - stdValues[j][k], 2);
		    	 }
		    }
		    stdDev[i] = (double)Math.round(10*(Math.sqrt(sum/(stencil*stencil))))/10;
		}
		System.out.println("Color values measured");
	}
	
	// The noise has to be measured in the luminance channel Y
	public void measurePatchesLAB(int stencil, String filename, String imageMagick,
			String workingDir) throws IMException {
		
		// Build the RGB filename
		String filenameRGB = "rgb.tif";
		
		// Delete old file, we use the existence of the file to 
		// check if ImageMagick worked correctly
		// Previously just a slash without color ?!?
		File color = new File(workingDir + "/color.txt");
		if (color.exists()){
			color.delete();
		}
		
		// Change to other profile later!!
		try {
			Process p = Runtime.getRuntime().exec
			(imageMagick + "convert " + filename +  " -profile " + workingDir + "/" + "sRGB.icc "  +
					 workingDir + "/" + filenameRGB);
			System.out.println(imageMagick + "convert " + filename +  " -profile " + workingDir + "/" + "sRGB.icc "  +
					 workingDir + "/" + filenameRGB);
			// Wait until ImageMagick finishes
			p.waitFor();
		}
		catch (Exception err) {
			err.printStackTrace();
		}
		
		for (int i = 0; i < numPatches; i++) {
			// Run Imagemagick, the output is stored in color.txt. 
			// It's impossible to get the result to stdout.
			try {
				Process p = Runtime.getRuntime().exec
				(imageMagick + "convert -crop " + stencil +"x" + stencil + "+" + 
						(patchLocations[i][0] + stencil/2) + "+" + 
						(patchLocations[i][1] + stencil/2) + " " + workingDir + "/" 
						+ filenameRGB + " +repage " + workingDir + "/color.txt");
				// Wait until ImageMagick finishes
				p.waitFor();
			}
			catch (Exception err) {
				err.printStackTrace();
			}
			
			// If color.txt doesn't exist, throw an exception as this indicates
			// errors in the file. This is not a pretty solution but I don't 
			// know how to check ImageMagick
			if (!color.exists()) {
				throw new IMException();
			}
			
			// Read the color values  from the output file and 
			// compute the values for the standard deviation (noise)
			// Delimiters to parse the output
			String delimiters = "[ .,?!£\\(\\)]+";
			double stdValues[][] = new double[3][stencil*stencil];
			try {
				BufferedReader input =  new BufferedReader(
						new FileReader(workingDir + "/color.txt"));
				try {
					String line = null; 
					int j = 0;
					// Read each line and parse the data
					while (( line = input.readLine()) != null){
						String[] tokens = null;
						tokens = line.split(delimiters);
						// First line in the file starts with "# ImageMagick pixel..."
						if (!tokens[0].equals("#")) {
							// The first two tokens from each line are coordinates

							stdValues[0][j] = Double.valueOf(tokens[2]);
							stdValues[1][j] = Double.valueOf(tokens[3]);
							stdValues[2][j] = Double.valueOf(tokens[4]);
							j++;
						}	        	
					}
				}
				finally {
					input.close();
		      }
		    }
		    catch (IOException ex){
		      ex.printStackTrace();
		    }
		    catch (ArrayIndexOutOfBoundsException ex){
			      ex.printStackTrace();
			}
		}
	}
	
	public void computeResolution() {
		double a, b;
		a = Math.pow(targetLocation[1][0]- targetLocation[0][0], 2);
		b = Math.pow(targetLocation[1][1]- targetLocation[0][1], 2);
		targetLength = Math.round(Math.sqrt(a+b));
		resolution = (double)Math.round((100*targetLength/(targetSizeOriginal[0]/2.54)))/100;
		System.out.println("Resolution: " + resolution + " ppi");
	}
	
	// Write information to imageData.xml
	public void setImageData(int stencil, String imageDataFilename, String imageQuality) 
		throws ParserConfigurationException, SAXException, IOException, 
		XPathExpressionException {

	    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
	    domFactory.setNamespaceAware(true); // never forget this!
	    
	    DocumentBuilder builder = domFactory.newDocumentBuilder();
	    Document doc = builder.parse(imageDataFilename);
	    doc.getDocumentElement().normalize();
	    XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
	      
	    // Set measured LAB-values of patches
	    if (colorSpace.equals("LAB")) {
	    	XPathExpression expr = xpath.compile("//patch/colorValues/LAB/*");
	 	    Object result = expr.evaluate(doc, XPathConstants.NODESET);
	 	    NodeList nodes = (NodeList) result;
	    	int j = 0;
	    	for (int i = 0; i < numPatches*3; i++) {
	    		nodes.item(i).setTextContent(Double.toString(measuredColorValues[j][0]));
	    		nodes.item(++i).setTextContent(Double.toString(measuredColorValues[j][1]));
	    		nodes.item(++i).setTextContent(Double.toString(measuredColorValues[j][2]));
	    		j++; 
	    	}
	    	// Set deltaE
	    	expr = xpath.compile("//patch/deltaE");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	for (int i = 0; i < numPatches; i++) {
	    		nodes.item(i).setTextContent(Double.toString(deltaE[i]));
	    	}
	    
	    	// Set deltaL
	    	expr = xpath.compile("//patch/deltaL");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	if (startWithGrayscale == 1) {
	    		// patchesBeforeChange = 0 when we don't have any color patches 
	    		// -> more logical description of the target 
	    		if (patchesBeforeChange == 0)
	    			patchesBeforeChange = numPatches;
	    		for (int i = 0; i < patchesBeforeChange; i++) 
	    			nodes.item(i).setTextContent(Double.toString(deltaL[i]));
	    		}
	    	else {
	    		for (int i = patchesBeforeChange; i < numPatches; i++) 
	    			nodes.item(i).setTextContent(Double.toString(deltaL[i]));
	    	}	
	    
	    	// Set deltaC
	    	expr = xpath.compile("//patch/deltaC");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;		
	    	if (startWithGrayscale == 1) {
	    		// patchesBeforeChange = 0 when we don't have any color patches 
	    		// -> more logical description of the target 
	    		if (patchesBeforeChange == 0)
	    			patchesBeforeChange = numPatches;
	    		for (int i = 0; i < patchesBeforeChange; i++) 
	    			nodes.item(i).setTextContent(Double.toString(deltaC[i]));
	    		}
	    	else {
	    		for (int i = patchesBeforeChange; i < numPatches; i++) 
	    			nodes.item(i).setTextContent(Double.toString(deltaC[i]));
	    	}	
	    		
	    	// Set maxDeltaE
	    	expr = xpath.compile("//LAB/maxDeltaE");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	for (int i = 0; i < nodes.getLength(); i++) {
	    		nodes.item(i).setTextContent(Double.toString(maxDeltaE));
	    	}
	    	// Set meanDeltaE
	    	expr = xpath.compile("//LAB/meanDeltaE");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	for (int i = 0; i < nodes.getLength(); i++) {
	    		nodes.item(i).setTextContent(Double.toString(meanDeltaE));
	    	}
	    	// Set maxDeltaL
	    	expr = xpath.compile("//LAB/maxDeltaL");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	for (int i = 0; i < nodes.getLength(); i++) {
	    		nodes.item(i).setTextContent(Double.toString(maxDeltaL));
	    	}
	    	// Set meanDeltaL
	    	expr = xpath.compile("//LAB/meanDeltaL");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	for (int i = 0; i < nodes.getLength(); i++) {
	    		nodes.item(i).setTextContent(Double.toString(meanDeltaL));
	    	}
	    	// Set maxDeltaC
	    	expr = xpath.compile("//LAB/maxDeltaC");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	for (int i = 0; i < nodes.getLength(); i++) {
	    		nodes.item(i).setTextContent(Double.toString(maxDeltaC));
	    	}
	    	// Set meanDeltaC
	    	expr = xpath.compile("//LAB/meanDeltaC");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	for (int i = 0; i < nodes.getLength(); i++) {
	    		nodes.item(i).setTextContent(Double.toString(meanDeltaC));
	    	}
	    	
	    	// Set gainModulation
		    expr = xpath.compile("//gainModulation/L95-L90");
		    result = expr.evaluate(doc, XPathConstants.NODESET);
		    nodes = (NodeList) result;
		    if (gainModulation[0] > 0)
		       nodes.item(0).setTextContent(Double.toString(gainModulation[0]));
		    expr = xpath.compile("//gainModulation/L90-L85");
		    result = expr.evaluate(doc, XPathConstants.NODESET);
		    nodes = (NodeList) result;
		    if (gainModulation[1] > 0)
		       nodes.item(0).setTextContent(Double.toString(gainModulation[1]));
		    expr = xpath.compile("//gainModulation/L85-L25");
		    result = expr.evaluate(doc, XPathConstants.NODESET);
		    nodes = (NodeList) result;
		    if (gainModulation[2] > 0)
		       nodes.item(0).setTextContent(Double.toString(gainModulation[2]));
		    expr = xpath.compile("//gainModulation/L85-L10");
		    result = expr.evaluate(doc, XPathConstants.NODESET);
		    nodes = (NodeList) result;
		    if (gainModulation[3] > 0)
		       nodes.item(0).setTextContent(Double.toString(gainModulation[3]));
	    }
	    // Set measured RGB values
	    else {
	    	XPathExpression expr = xpath.compile("//patch/colorValues/adobeRGB/*");
	 	    Object result = expr.evaluate(doc, XPathConstants.NODESET);
	 	    NodeList nodes = (NodeList) result;
	    	int j = 0;
	    	for (int i = 0; i < numPatches; i++) {
	    		nodes.item(i).setTextContent(Double.toString(measuredColorValues[j][0]));
	    		nodes.item(++i).setTextContent(Double.toString(measuredColorValues[j][1]));
	    		nodes.item(++i).setTextContent(Double.toString(measuredColorValues[j][2]));
	    		j++; 
	    	}
	   
	    	expr = xpath.compile("//deviationRGB/*");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	j = 0;
	    	for (int i = 0; i < numPatches; i++) {
	    		nodes.item(i).setTextContent(Integer.toString(deviationRGB[j][0]));
	    		nodes.item(++i).setTextContent(Integer.toString(deviationRGB[j][1]));
	    		nodes.item(++i).setTextContent(Integer.toString(deviationRGB[j][2]));
	    		j++; 
	    	}
	    	
	    	expr = xpath.compile("//RGB/meanDeviation");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	for (int i = 0; i < nodes.getLength(); i++) {
	    		nodes.item(i).setTextContent(Double.toString(meanDeviationRGB));
	    	} 	
	    	
	    	expr = xpath.compile("//RGB/maxDeviation");
	    	result = expr.evaluate(doc, XPathConstants.NODESET);
	    	nodes = (NodeList) result;
	    	for (int i = 0; i < nodes.getLength(); i++) {
	    		nodes.item(i).setTextContent(Integer.toString(maxDeviationRGB));
	    	} 	
	    }
	    
	    // Set target name
	    XPathExpression expr = xpath.compile("//generalData/nameOfTarget");
	    Object result = expr.evaluate(doc, XPathConstants.NODESET);
	    NodeList nodes = (NodeList) result;
	    nodes.item(0).setTextContent(targetName);
	    
	    // Set noise
    	expr = xpath.compile("//patch/noise");
    	result = expr.evaluate(doc, XPathConstants.NODESET);
    	nodes = (NodeList) result;
    	for (int i = 0; i < numPatches; i++) {
    		nodes.item(i).setTextContent(Double.toString(stdDev[i]));
    	}
	    
	    // Set date of processing
	    Calendar cal = Calendar.getInstance();
	    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	    expr = xpath.compile("//generalData/dateOfProcessing");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    nodes.item(0).setTextContent(df.format(cal.getTime()));
	    
	    // Set image quality
	    // Image quality is a file name, need to manipulate the string
	    // to remove the path
	    int start = 0;
	    for (int i = 0; i < imageQuality.length(); i++) {
	    	if (imageQuality.charAt(i)== '/' || imageQuality.charAt(i)=='\\') {
	    		start = i; 
	    	}
	    }
	    imageQuality = imageQuality.substring(start+1);
	    expr = xpath.compile("//generalData/qualityLevel");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    nodes.item(0).setTextContent(imageQuality);
	    
	    // Set size of the stencil
	    expr = xpath.compile("//generalData/sizeOfStencil");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	       nodes.item(i).setTextContent(Integer.toString(stencil) + "x" + 
	    		   Integer.toString(stencil));
	    }
	    
	    // Set the color space
	    expr = xpath.compile("//generalData/imageColorSpace");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	       nodes.item(i).setTextContent(colorSpace);
	    }
	    
	    // Set the length of the target
	    expr = xpath.compile("//lengthOfTarget");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	       nodes.item(i).setTextContent(Double.toString(targetLength));
	    }
	   
	    // Set the resolution of the target
	    expr = xpath.compile("//resolution");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	       nodes.item(i).setTextContent(Double.toString(resolution));
	    }
	    
	    // Write to file
	    try{ 
	    	// Delete existing file
	    	File f = new File(imageDataFilename);
	    	if (f.exists())
	    		f.delete();
	    
            // Creating new transformer factory
            javax.xml.transform.TransformerFactory factoryT = 
                         javax.xml.transform.TransformerFactory.newInstance();
 
            // Creating new transformer
            javax.xml.transform.Transformer transformer = 
                                                     factoryT.newTransformer();
 
            // Creating DomSource with Document you need to write to file
            javax.xml.transform.dom.DOMSource domSource = 
                           new javax.xml.transform.dom.DOMSource(doc);
 
            // Creating Output Stream to write XML Content
            java.io.ByteArrayOutputStream bao = 
                           new java.io.ByteArrayOutputStream();
 
            // Transforming domSource and getting out put in output stream
            transformer.transform(domSource, 
                       new javax.xml.transform.stream.StreamResult(bao));
 
            // writing output stream data to file
            java.io.FileOutputStream fos = new 
            	java.io.FileOutputStream(imageDataFilename);
            fos.write(bao.toByteArray());
            fos.flush();
            fos.close();
 
        } catch (java.lang.Exception ex) {
            //TODO: necessary exception handling 
        }
	 
	}
	
	// Read number of patches from rulerData.xml
	public void readNumPatches(String targetDataFilename) throws ParserConfigurationException, 
		SAXException, IOException, XPathExpressionException {

	    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
	    domFactory.setNamespaceAware(true); // never forget this!
	    
	    DocumentBuilder builder = domFactory.newDocumentBuilder();
	    Document doc = builder.parse(targetDataFilename);

	    XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
	    
	    // Get number of patches
	    XPathExpression expr = xpath.compile("//numberOfPatches/text()");
	    Object result = expr.evaluate(doc, XPathConstants.NODESET);
	    NodeList nodes = (NodeList) result;
	    numPatches = Integer.valueOf(nodes.item(0).getNodeValue());
	    System.out.println("NumPatches read: " + numPatches);
	    
	}
	
	// Read data from rulerData.xml
	public void readTargetData(String targetDataFilename) throws ParserConfigurationException, 
		SAXException, IOException, XPathExpressionException {

	    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
	    domFactory.setNamespaceAware(true); // never forget this!
	    
	    DocumentBuilder builder = domFactory.newDocumentBuilder();
	    Document doc = builder.parse(targetDataFilename);

	    XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
	    
	    // Read LAB-values
	    // Get L-values
	    XPathExpression expr 
	     = xpath.compile("//LAB/L/text()");
	    Object result = expr.evaluate(doc, XPathConstants.NODESET);
	    NodeList nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	        referenceColorValuesLAB[i][0] = Double.valueOf(nodes.item(i).getNodeValue());
	    }
	    
	    // Get A-values
	    expr = xpath.compile("//LAB/A/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	        referenceColorValuesLAB[i][1] = Double.valueOf(nodes.item(i).getNodeValue());
	    }
	    
	    // Get B-values
	    expr = xpath.compile("//LAB/B/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	        referenceColorValuesLAB[i][2] = Double.valueOf(nodes.item(i).getNodeValue());
	    }
	    System.out.println("Reference color values read (LAB)");
	    
	    // Read RGB-values
	    // Get R-values
	    expr = xpath.compile("//adobeRGB/R/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	        referenceColorValuesRGB[i][0] = Integer.valueOf(nodes.item(i).getNodeValue());
	    }
	    
	    // Get G-values
	    expr = xpath.compile("//adobeRGB/G/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	        referenceColorValuesRGB[i][1] = Integer.valueOf(nodes.item(i).getNodeValue());
	    }
	    
	    // Get B-values
	    expr = xpath.compile("//adobeRGB/B/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	        referenceColorValuesRGB[i][2] = Integer.valueOf(nodes.item(i).getNodeValue());
	    }
	    System.out.println("Reference color values read (RGB)");
	 
	    // Get startWithGrayscale
	    expr = xpath.compile("//generalData/startWithGrayscale/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    startWithGrayscale = Integer.valueOf(nodes.item(0).getNodeValue());
	
	    // Get startWithGrayscale
	    expr = xpath.compile("//generalData/patchesBeforeChange/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    patchesBeforeChange = Integer.valueOf(nodes.item(0).getNodeValue());
	    
	    // Get size of target
	    expr = xpath.compile("//sizeRealWorld/X/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    targetSizeOriginal[0] = Double.valueOf(nodes.item(0).getNodeValue());
	    expr = xpath.compile("//sizeRealWorld/Y/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    targetSizeOriginal[1] = Double.valueOf(nodes.item(0).getNodeValue());
	    System.out.println("Target size: " + targetSizeOriginal[0] + "x" +
	    		targetSizeOriginal[1] + " cm");
	    
	    // Get L*95
	    expr = xpath.compile("//gainModulation/L95/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    gainModulationPatches[0] = Integer.valueOf(nodes.item(0).getNodeValue());
	    
	    // Get L*90
	    expr = xpath.compile("//gainModulation/L90/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    gainModulationPatches[1] = Integer.valueOf(nodes.item(0).getNodeValue());
	    
	    // Get L*85
	    expr = xpath.compile("//gainModulation/L85/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    gainModulationPatches[2] = Integer.valueOf(nodes.item(0).getNodeValue());
	    
	    // Get L*80
	    expr = xpath.compile("//gainModulation/L80/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    gainModulationPatches[3] = Integer.valueOf(nodes.item(0).getNodeValue());
	    
	    // Get L*25
	    expr = xpath.compile("//gainModulation/L25/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    gainModulationPatches[4] = Integer.valueOf(nodes.item(0).getNodeValue());

	    // Get L*20
	    expr = xpath.compile("//gainModulation/L20/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    gainModulationPatches[5] = Integer.valueOf(nodes.item(0).getNodeValue());
	    
	    // Get L*10
	    expr = xpath.compile("//gainModulation/L10/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    gainModulationPatches[6] = Integer.valueOf(nodes.item(0).getNodeValue());
	    
	    // Get L*5
	    expr = xpath.compile("//gainModulation/L5/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    gainModulationPatches[7] = Integer.valueOf(nodes.item(0).getNodeValue());
	    
	    // Get target name
	    expr = xpath.compile("//generalData/name/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    targetName = String.valueOf(nodes.item(0).getNodeValue());
	}
	
	// Read data from imageData.xml
	public void readImageData(String imageDataFilename) throws 
		ParserConfigurationException, SAXException, IOException, 
		XPathExpressionException {

	    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
	    domFactory.setNamespaceAware(true); // never forget this!
	    
	    DocumentBuilder builder = domFactory.newDocumentBuilder();
	    Document doc = builder.parse(imageDataFilename);

	    XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
	    
	    // Get x-values of patches
	    XPathExpression expr 
	     = xpath.compile("//patchData/patch/center/X/text()");
	    Object result = expr.evaluate(doc, XPathConstants.NODESET);
	    NodeList nodes = (NodeList) result;
	    for (int i = 0; i < numPatches; i++) {
	        patchLocations[i][0] = Integer.valueOf(nodes.item(i).getNodeValue());
	    }
	    
	    // Get y-values of patches
	    expr = xpath.compile("//patchData/patch/center/Y/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < numPatches; i++) {
	        patchLocations[i][1] = Integer.valueOf(nodes.item(i).getNodeValue());
	    }
	    System.out.println("Patch positions read from file");
	    
	    // Get x-values of target position
	    expr = xpath.compile("//corner/X/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	        targetLocation[i][0] = Integer.valueOf(nodes.item(i).getNodeValue());
	    }
	    
	    // Get y-values of target position
	    expr = xpath.compile("//corner/Y/text()");
	    result = expr.evaluate(doc, XPathConstants.NODESET);
	    nodes = (NodeList) result;
	    for (int i = 0; i < nodes.getLength(); i++) {
	        targetLocation[i][1] = Integer.valueOf(nodes.item(i).getNodeValue());
	    }
	}
	
	// Run the C program that computes the patch locations
	public static void computePatchLocation(String imageName, 
			String imageDataTemplateFilename, String imageDataFilename, 
			String targetName, String dataName, String outputImage, 
			String find) throws FindException{
		int findResult=0;
		/*System.out.println(find + " " + targetName + " " + imageName + " " + dataName + " " + 
				imageDataTemplateFilename + " " + imageDataFilename);*/
		String line = new String();
		try {
			System.out.println("Calling 'Find' to compute the patch locations");
			Process p = Runtime.getRuntime().exec
			(find + " " + targetName + " " + imageName + " " + dataName + " " + 
					imageDataTemplateFilename + " " + imageDataFilename + " " +
					outputImage);
			
			BufferedReader input =
				new BufferedReader
				(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null) {
				System.out.println(line);
			}			
			input.close();
			findResult = p.waitFor();
		}
		catch (Exception err) {
			err.printStackTrace();
		}
		if (findResult == -1)
			throw new FindException();
		System.out.println("Patch locations computed");
	}
	
	public String getColorSpace() {
		return colorSpace;
	}
	
	public int getNumPatches() {
		return numPatches;
	}
	
	public int getStartWithGrayscale() {
		return startWithGrayscale;
	}
	
	public int getPatchesBeforeChange() {
		return patchesBeforeChange;
	}
	
	public double getDeltaL(int i) {
		return deltaL[i];
	}
	
	public double getMeasuredColorValue(int i, int j) {
		return measuredColorValues[i][j];
	}

	public double getReferenceColorValue(int i, int j) {
		if (colorSpace.equals("LAB"))
			return referenceColorValuesLAB[i][j];
		else 
			return referenceColorValuesRGB[i][j];
	}
	
	public int getDeviationRGB(int i, int j) {
		return deviationRGB[i][j];
	}
	
	public double getGM(int i) {
		return gainModulation[i];
	}
	
	public String getGMName(int i) {
		return gainModulationName[i];
	}
	
	public double getResolution() {
		return resolution;
	}
	
	public double getDeltaE(int i) {
		return deltaE[i];
	}

	public double getMeanDeltaE() {
		return meanDeltaE;
	}
	
	public double getDeltaC(int i) {
		return deltaC[i];
	}
	
	public double getStdDev(int i) {
		return stdDev[i];
	}
	
	public static void main(String[] args) throws XPathExpressionException, 
		ParserConfigurationException, SAXException, IOException, IMException,
		FindException{
		
		// Size of stencil and filenames should be supplied in GUI
		int stencil = 5;
		String imageName = "C:\\Programmering\\Colorite_demo\\referenceOS14MultipleTargets.tif";
		//String targetName = "C:\\Diverse\\Kvalité\\Test_plan5\\kodakTarget.png";
		String targetName = "C:\\Programmering\\Colorite_demo\\CC_SG.png";
		String imageDataFilename = "C:\\Programmering\\Colorite_demo\\imageData.xml";
		String imageQuality = "C:\\Programmering\\Colorite_demo\\qualityLevel1.xml";
		String imageDataTemplateFilename = "C:\\Programmering\\Colorite_demo\\imageDataTemplate.xml";
		//String targetDataFilename = "C:\\Programmering\\Matchning\\targetKodak.xml";
		String targetDataFilename = "C:\\Programmering\\Colorite_demo\\CC_SG_MKC_measured.xml";
		String find = "C:\\Programmering\\Projects\\VS\\find3\\Release\\Find.exe";
		String imageMagick = "C:\\Program Files\\ImageMagick-6.6.3-Q16\\";
		String workingDir = "C:\\Programmering\\Matchning\\";
		String outputImage = "C:\\Programmering\\Matchning\\output.png";
		
		Measure currentImage = new Measure(imageName, targetName, 
				imageDataTemplateFilename, imageDataFilename, targetDataFilename, 
				outputImage, find, imageMagick);
		
		currentImage.measurePatches(stencil, imageName, imageMagick, workingDir);
		if (currentImage.colorSpace.equals("LAB")) {
			currentImage.computeGainModulation();
			currentImage.computeDeltaE();
			currentImage.computeDeltaC();
			currentImage.computeDeltaL();
		}
		else 			
			currentImage.computeDeviationRGB();	
		currentImage.setImageData(stencil, imageDataFilename, imageQuality);
		
		
	}		
}