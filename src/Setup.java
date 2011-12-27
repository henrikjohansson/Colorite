import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.awt.Image;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JLabel;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

// TODO Javadoc!
// TODO Add check on last calibration. Store interval in inputFile.txt
// TODO Add scale in XML (1:1 etc)
// TODO Checkbox to see matched image
// TODO Exception handling for input files
// TODO Specify filenames for output.
// TODO Button to view xml-code
// TODO Låta utfil namnges av infil!
// TODO Check for correct XML-files, not just an XML-file
// TODO Working dir for config.txt
// TODO Check file types in batch mode
// TODO Small extern program to set config.txt
// TODO Convert DNG to tiff

public class Setup extends JFrame {

	private Toolkit toolkit;
	private JPanel panel;
	private String referenceImage;
	private String targetImage;
	private String targetData;
	private String qualityData;
	private String outputImage;
	private String useGUI;
	private String useBatchMode;
	private int showImage;
	private JTextField referenceField;
	private JTextField targetField;
	private JTextField targetDataField;
	private JTextField qualityField;
	private JCheckBox checkBox;
	private Measure currentImage;
	private String imageDataTemplateFilename;


	public Setup() {

		toolkit = getToolkit();
		final Dimension size = toolkit.getScreenSize();
		
		// Default file names
		referenceImage = "C:\\Programmering\\Matchning\\sceneLAB.tif";
		targetImage = "C:\\Programmering\\Matchning\\Linjal.png";
		targetData = "C:\\Programmering\\Matchning\\rulerData.xml";
		targetData = "C:\\Programmering\\Matchning\\qualityLevel1.xml";
		outputImage = "C:\\Programmering\\Matchning\\output.png";
		// Read image files location
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(System.getProperty("user.dir") + "/config.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			if ((strLine = br.readLine()) != null)   {			
				referenceImage = strLine;
			}
			if ((strLine = br.readLine()) != null)   {			
				targetImage = strLine;
			}
			if ((strLine = br.readLine()) != null)   {			
				targetData = strLine;
			}
			if ((strLine = br.readLine()) != null)   {			
				outputImage = strLine;
			}
			if ((strLine = br.readLine()) != null)   {			
				qualityData = strLine;
			}
			if ((strLine = br.readLine()) != null)   {			
				useGUI = strLine;
			}
			if ((strLine = br.readLine()) != null)   {			
				useBatchMode = strLine;
			}
			
			in.close();
		} catch (IOException e1) {
			toolkit = getToolkit();
			//size = toolkit.getScreenSize();
			String text = "config.txt error";
			Utils.errorWindow(size.width, size.height-400, 200, 100, text);
		}
		
		// Check for GUI (never GUI in batch mode)
		if (useGUI.equals("1") || useGUI.equals("useGUI"))
			GUI();
		// No GUI
		else if (!(useBatchMode.equals("1") || useBatchMode.equals("useBatch")))
			run();
		else
			batchMode();
	}

	// Returns true if a space is found in the filename
	public boolean checkForSpaces(String filename) {
		boolean space = false;
		// Check for spaces
		for (int j = 0; j < filename.length(); j++) {
			if (filename.charAt(j) == (' '))
				space = true;
		}
		// Show error window if a space is found
		if (space) 
		{
			toolkit = getToolkit();
			Dimension size = toolkit.getScreenSize();
			String text = "No space allowed in filename";
			Utils.errorWindow(size.width, size.height, 200, 100,
					text);
		}
		return space;
	}
	
	// Run in batch mode
	public void batchMode(){
		File files[]; 
		int fileTypeStart = 0;
		
		// List all files in directory
		File directory = new File(referenceImage);
	    files = directory.listFiles();
	    boolean extension, space;
	    // Check for the last dot in the filename
	    for (int i = 0, n = files.length; i < n; i++) {
	    	referenceImage = files[i].toString();
	    	extension = false; space = false;
	    	for (int j = 0; j < referenceImage.length(); j++) {
				if (referenceImage.charAt(j) == ('.')) {
					fileTypeStart = j;
					extension = true;
				}
				if (referenceImage.charAt(j) == (' '))
					space = true;
			}
	  
	    	// Assure that we only measure on support image formats and that we
	    	// have an extension, i.e. dot in the filename
	    	if (extension && !space) {
	    		String fileType = referenceImage.substring(fileTypeStart+1);
	    		if (fileType.equals("tif") || fileType.equals("dng") || 
	    				fileType.equals("jpg") || fileType.equals("jpeg") ||
	    				fileType.equals("png"))	{
	    			System.out.println("Now measuring: " + referenceImage);
	    			run();    
	    		}
	    	}
	    }
	}
	
	// Run with GUI enabled
	public void GUI(){

		setTitle("Colorite: Setup");
		setSize(800, 200);

		// Center on screen
		toolkit = getToolkit();
		final Dimension size = toolkit.getScreenSize();
		setLocation((size.width - getWidth()) / 2,
				(size.height - getHeight()) / 2);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		panel = new JPanel();
		getContentPane().add(panel);
		panel.setLayout(new GridLayout(5, 3, 5, 5));
		
		/***********************
		 * Set reference image
		 ************************/

		JLabel label = new JLabel("Reference image:");
		panel.add(label);

		// Set default reference image
		// Should sent to program later on!

		referenceField = new JTextField(30);
		referenceField.setText(referenceImage);
		panel.add(referenceField);
		referenceField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				File tempFile = new File(referenceField.getText());
				// Check if file exists
				if (tempFile.exists()) {
					if (!checkForSpaces(referenceImage))
						referenceImage = referenceField.getText();
					else
						referenceField.setText(referenceImage);
				} else {
					toolkit = getToolkit();
					Dimension size = toolkit.getScreenSize();
					String text = "Reference image does not exist";
					Utils.errorWindow(size.width, size.height, 200, 100, text);
					referenceField.setText(referenceImage);
				}
			}
		});

		// Change reference image using JFileChooser
		JButton changeReferenceImage = new JButton("Change");
		changeReferenceImage.setBounds(50, 60, 80, 30);
		panel.add(changeReferenceImage);
		changeReferenceImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JFileChooser reference = new JFileChooser();
				String workingDir = System.getProperty("user.dir");
				File current = new File(workingDir);
				reference.setCurrentDirectory(current);
				FileFilter filter = new FileNameExtensionFilter("tiff-images",
				"tif");
				reference.addChoosableFileFilter(filter);
				int ret = reference.showDialog(panel, "Select");
				if (ret == JFileChooser.APPROVE_OPTION) {
					String referenceTemp = referenceImage;
					File file = reference.getSelectedFile();	
					// Check if file exists
					if (file.exists()) {
						referenceImage = file.getAbsolutePath();
						// Check for spaces
						if (!checkForSpaces(referenceImage)) {
							referenceField.setText(referenceImage);
						}
						else {
							referenceImage = referenceTemp;
							referenceField.setText(referenceImage);
						}
							
					// Error if file doesn't exist
					} else {
						toolkit = getToolkit();
						Dimension size = toolkit.getScreenSize();
						String text = "Reference file does not exist";
						Utils.errorWindow(size.width, size.height, 200, 100,
								text);
					}
				}
			}
		});

		/***********************
		 * Set target image
		 ************************/

		JLabel targetLabel = new JLabel("Target image:");
		panel.add(targetLabel);

		// Set default target image
		targetField = new JTextField(30);
		targetField.setText(targetImage);
		panel.add(targetField);
		targetField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

				String targetTemp = targetField.getText();
				File tempFile = new File(targetTemp);
				// Check if file exists
				if (tempFile.exists()) {
					if (!checkForSpaces(targetTemp))
						targetImage = targetField.getText();
					else
						targetField.setText(targetImage);
				} else {
					toolkit = getToolkit();
					Dimension size = toolkit.getScreenSize();
					String text = "Target image does not exist";
					Utils.errorWindow(size.width, size.height, 200, 100, text);
					targetField.setText(targetImage);
				}
			}
		});

		// Change target image using JFileChooser
		JButton changeTargetImage = new JButton("Change");
		changeTargetImage.setBounds(50, 60, 80, 30);
		panel.add(changeTargetImage);
		changeTargetImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JFileChooser target = new JFileChooser();
				String workingDir = System.getProperty("user.dir");
				File current = new File(workingDir);
				target.setCurrentDirectory(current);
				FileFilter filter = new FileNameExtensionFilter("png-images",
				"png");
				target.addChoosableFileFilter(filter);

				int ret = target.showDialog(panel, "Select");
				if (ret == JFileChooser.APPROVE_OPTION) {
					String targetTemp = targetImage;
					File file = target.getSelectedFile();
					// Check if file exists
					if (file.exists()) {
						targetImage = file.getAbsolutePath();
						if (!checkForSpaces(targetImage))
							targetField.setText(targetImage);
						else {
							targetImage = targetTemp;
							targetField.setText(targetImage);
						}
					} else {
						toolkit = getToolkit();
						Dimension size = toolkit.getScreenSize();
						String text = "Target image does not exist";
						Utils.errorWindow(size.width, size.height, 200, 100,
								text);
					}
				}
			}
		});

		/***********************
		 * Set target data file
		 ************************/

		JLabel targetDataLabel = new JLabel("Target data:");
		panel.add(targetDataLabel);

		targetDataField = new JTextField(30);
		targetDataField.setText(targetData);
		panel.add(targetDataField);
		targetDataField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

				String dataTemp = targetDataField.getText();
				File tempFile = new File(dataTemp);
				// Check if file exists
				if (tempFile.exists()) {
					targetData = targetDataField.getText();
				} else {
					toolkit = getToolkit();
					Dimension size = toolkit.getScreenSize();
					String text = "Target data does not exist";
					Utils.errorWindow(size.width, size.height, 200, 100, text);
					targetDataField.setText(targetData);
				}
			}
		});

		// Change target data file using JFileChooser
		JButton changeTargetData = new JButton("Change");
		changeTargetData.setBounds(50, 60, 80, 30);
		panel.add(changeTargetData);
		changeTargetData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JFileChooser targetXML = new JFileChooser();
				String workingDir = System.getProperty("user.dir");
				File current = new File(workingDir);
				targetXML.setCurrentDirectory(current);
				FileFilter filter = new FileNameExtensionFilter("xml-files",
				"xml");
				targetXML.addChoosableFileFilter(filter);

				int ret = targetXML.showDialog(panel, "Select");
				if (ret == JFileChooser.APPROVE_OPTION) {
					File file = targetXML.getSelectedFile();
					// Check if file exists
					if (file.exists()) {
						targetData = file.getAbsolutePath();
						targetDataField.setText(targetData);
					} else {
						toolkit = getToolkit();
						Dimension size = toolkit.getScreenSize();
						String text = "Target data does not exist";
						Utils.errorWindow(size.width, size.height, 200, 100,
								text);
					}
				}
			}
		});

		/***********************
		 * Set image quality file
		 ************************/
		JLabel qualityLabel = new JLabel("Image quality:");
		panel.add(qualityLabel);

		qualityField = new JTextField(30);
		qualityField.setText(qualityData);
		panel.add(qualityField);
		qualityField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

				String dataTemp = qualityField.getText();
				File tempFile = new File(dataTemp);
				// Check if file exists
				if (tempFile.exists()) {
					qualityData = qualityField.getText();
				} else {
					toolkit = getToolkit();
					Dimension size = toolkit.getScreenSize();
					String text = "Quality data doesn't exist";
					Utils.errorWindow(size.width, size.height, 200, 100, text);
					qualityField.setText(qualityData);
				}
			}
		});

		// Change quality data file using JFileChooser
		JButton changeImageQuality = new JButton("Change");
		changeImageQuality.setBounds(50, 60, 80, 30);
		panel.add(changeImageQuality);
		changeImageQuality.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JFileChooser qualityXML = new JFileChooser();
				String workingDir = System.getProperty("user.dir");
				File current = new File(workingDir);
				qualityXML.setCurrentDirectory(current);
				FileFilter filter = new FileNameExtensionFilter("xml-files",
				"xml");
				qualityXML.addChoosableFileFilter(filter);

				int ret = qualityXML.showDialog(panel, "Select");
				if (ret == JFileChooser.APPROVE_OPTION) {
					File file = qualityXML.getSelectedFile();
					// Check if file exists
					if (file.exists()) {
						qualityData = file.getAbsolutePath();
						qualityField.setText(qualityData);
					} else {
						toolkit = getToolkit();
						Dimension size = toolkit.getScreenSize();
						String text = "Image quality data does not exist";
						Utils.errorWindow(size.width, size.height, 200, 100,
								text);
					}
				}
			}
		});

		showImage = 0;
		checkBox = new JCheckBox("Show reference image when finished");
		checkBox.setEnabled(true);
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (checkBox.isSelected())
					showImage = 1;
				else
					showImage = 0;
			}
		});

		panel.add(checkBox);
		panel.add(new JLabel(""));

		/*
		 * Run button
		 */
		JButton runButton = new JButton("Run");
		runButton.setBounds(50, 60, 80, 30);
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				run();
			}
		});
		panel.add(runButton);
	}

	public void run(){
		int stencil = 5;

		String imageDataFilename = null;
		String find = null;
		String imageMagick = null;
		String workingDir = System.getProperty("user.dir");
		try {
			// Read programLocations from file
			FileInputStream fstream = new FileInputStream(System.getProperty("user.dir") + "/programLocations.txt");
			//FileInputStream fstream = new FileInputStream("C:\\Programmering\\Matchning\\programLocations.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			if ((strLine = br.readLine()) != null)   {			
				imageDataTemplateFilename = strLine;
			}
			if ((strLine = br.readLine()) != null)   {			
				imageDataFilename = strLine;
			}
			if ((strLine = br.readLine()) != null)   {			
				find = strLine;
			}
			if ((strLine = br.readLine()) != null)   {			
				imageMagick = strLine;
			}

			in.close();
			
			// Modify the imageData and output filename for batchRun
			if (!(useGUI.equals("1") || useGUI.equals("useGUI")) && 
					(useBatchMode.equals("1") || useBatchMode.equals("useBatch"))) {
				// Add iQ to the right part of the filename
				int nameStart = 0;
				for (int i = 0; i < referenceImage.length(); i++) {
					if (referenceImage.charAt(i) == ('/') || 
							referenceImage.charAt(i) == ('\\')) {
						nameStart = i;
					}
				}
				
				String start = referenceImage.substring(0, nameStart+1);
				String end = referenceImage.substring(nameStart + 1);
				imageDataFilename = start + "iQ_" + end;
				outputImage = start + "out_" + end;
				for (int i = 0; i < imageDataFilename.length(); i++) {
					if (imageDataFilename.charAt(i) == ('.')) {
						nameStart = i;
					}
				}
				imageDataFilename = imageDataFilename.substring(0, nameStart) + ".xml";	
				outputImage = outputImage.substring(0, nameStart+1) + ".png";	
			
				System.out.println("XML-output: " + imageDataFilename);
			}
			
			currentImage = new Measure(referenceImage, targetImage, 
					imageDataTemplateFilename, imageDataFilename, 
					targetData, outputImage, find, imageMagick);
			currentImage.measurePatches(stencil, referenceImage,
					imageMagick, workingDir);
			if (currentImage.getColorSpace().equals("LAB")) {
				currentImage.computeDeltaE();
				currentImage.computeDeltaL();
				currentImage.computeDeltaC();
				currentImage.computeGainModulation();
			}
			else
				currentImage.computeDeviationRGB();
			currentImage.measurePatchesLAB(stencil, referenceImage, imageMagick, workingDir);
			currentImage.setImageData(stencil, imageDataFilename, qualityData);
			
			// Only show result window if GUI is enabled
			if (useGUI.equals("1") || useGUI.equals("useGUI")) {
				Result result = new Result(currentImage, qualityData, targetData,
						referenceImage);

				result.setVisible(true);

				if (showImage == 1) {
					ShowImage image = new ShowImage(outputImage);
					image.setVisible(true);
				}
			}
		} catch (XPathExpressionException e) {
			toolkit = getToolkit();
			Dimension size = toolkit.getScreenSize();
			String text = "XPath error";
			Utils.errorWindow(size.width, size.height, 200, 100, text);
		} catch (ParserConfigurationException e) {
			toolkit = getToolkit();
			Dimension size = toolkit.getScreenSize();
			String text = "XML-parser error";
			Utils.errorWindow(size.width, size.height, 200, 100, text);
		} catch (SAXException e) {
			toolkit = getToolkit();
			Dimension size = toolkit.getScreenSize();
			String text = "Unable to read XML-data";
			Utils.errorWindow(size.width, size.height, 200, 100, text);
		} catch (IOException e) {
			toolkit = getToolkit();
			Dimension size = toolkit.getScreenSize();
			String text = "IO error";
			Utils.errorWindow(size.width, size.height, 200, 100, text);
		} catch (IMException e) {
			toolkit = getToolkit();
			Dimension size = toolkit.getScreenSize();
			String text = "ImageMagick returned empty";
			Utils.errorWindow(size.width, size.height, 200, 100, text);
		} catch (FindException e) {
			toolkit = getToolkit();
			Dimension size = toolkit.getScreenSize();
			String text = "Find unable to open image";
			Utils.errorWindow(size.width, size.height, 200, 100, text);
		}
	}
	
	public static void main(String[] args) {

		Setup setup = new Setup();
		if (setup.useGUI.equals("1") || setup.useGUI.equals("useGUI")) {
			setup.setVisible(true);
		}
	}
}
