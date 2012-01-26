/*
 Description: 
 
 A program that finds a target (color-checker) with color/grayscale 
 patches in a reference image. The center position of the patches
 are calculated. The program uses OpenCV for the image and libxml2 
 to store and read data in XML.
 
 Further documentation: 
 See xxx.doc for a complete walktrough of the geometrics used to 
 identify the patches
 
 Prerequisites Windows: 
 Install libxml2 in a location that is  in the PATH of the system. 
 libxlm2 also needs zlib and iconv to work. 
 
 Input: 
 1). Image file that only contains the target (color-checker). This image is
 used as the target in the matching-step. The image can be stored as 
 tif, png or jpg.
 2). Image file that contains the target (color-checker) in an arbitrary 
 size, position, and orientation. The image can be stored as tif, png 
 or jpg.
 3). File that specifies the number of patches and the center of 
 each patch on the target (color-checker). The sizes are given to 
 relative size of the target, with the upper-left corner as the origo.
 The file should be in xml and in the same format as the template 
 targetDataTemplate.xml. Data fields that doesn't contain information
 neccesary for the program can be empty. 
 4). Template file for the output. The file should be in xml and in the 
 same format as the template imageDataTemplate.xml.
 5). Filenames for the target file and image file, supplied as command line
 arguments. it's also possible to uncomment some limnes in main to use hard-coded 
 arguments. In Visual C++, the arguments are supplied using 
 Properties->Debugging->Command Arguments
 
 
 Output: 
 1). Location of the center of each patch, relative to the upper-left
 corner of the image.
 2). The location of the corners of the target, relative to the 
 upper-left corner of the image.
 3). The number of patches in the image
 4). The orientation of the target (if it's upsidedown)
 The output is formatted as xml and it's stored in the file imageData.xml
 
 To do:
 1). Checks for NULL pointer exceptions
 2). Checks for freed memory
 3). Handle errors
 */

// opencv.hpp new for mac!
#include <opencv.hpp>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <cmath>
#include <iostream>
#include <vector>
#include <xpath.h>

using namespace std;

// Reduce size of image by a factor two
IplImage* doPyrDown(IplImage* in, int filter)
{
	
	IplImage* out = cvCreateImage(cvSize( (int)in->width/2, (int)in->height/2 ),
								  in->depth, in->nChannels);
	cvPyrDown( in, out, filter );
	return( out );
}

string convertInt(int number)
{
	stringstream ss;
	ss << number;
	return ss.str();
}

// Get pointer to the XML-file
xmlDocPtr getDoc (char *docname) {
	xmlDocPtr doc;
	doc = xmlParseFile(docname);
	
	if (doc == NULL) {
		fprintf(stderr,"Document not parsed successfully. \n");
		return NULL;
	}
	return doc;
}

// Get the result of the XML-query 
xmlXPathObjectPtr getNodeSet (xmlDocPtr doc, xmlChar *xpath){
	
	xmlXPathContextPtr context;
	xmlXPathObjectPtr result;
	
	context = xmlXPathNewContext(doc);
	if (context == NULL) {
		printf("Error in xmlXPathNewContext\n");
		return NULL;
	}
	result = xmlXPathEvalExpression(xpath, context);
	xmlXPathFreeContext(context);
	if (result == NULL) {
		printf("Error in xmlXPathEvalExpression\n");
		return NULL;
	}
	if(xmlXPathNodeSetIsEmpty(result->nodesetval)){
		xmlXPathFreeObject(result);
        printf("No result\n");
		return NULL;
	}
	return result;
}

int getNumberOfPatches(xmlDocPtr doc, xmlChar *xPath) {
	xmlChar *num;
	int number;
	xmlNodeSetPtr nodeset;
	xmlXPathObjectPtr result;
	
	result = getNodeSet (doc, xPath);
	if (result) {
		nodeset = result->nodesetval;
		for (int i=0; i < nodeset->nodeNr; i++) {
			num = xmlNodeListGetString(doc, nodeset->nodeTab[i]->xmlChildrenNode, 1);
			number = atoi((char*)num);
			xmlFree(num);
		}
		xmlXPathFreeObject (result);
	}
	return number ;
}

// Get the center of the patches from the XML-file
void getTargetSize(xmlDocPtr doc, int &xSize, int &ySize, xmlChar *xPathOne, xmlChar *xPathTwo) {
	
	// Need to check for empty positions (NULL)!!
	xmlChar *num;
	xmlNodeSetPtr nodeset;
	xmlXPathObjectPtr result;
	
	// Get xSize
	result = getNodeSet (doc, xPathOne);
	if (result) {
		nodeset = result->nodesetval;
		for (int i=0; i < nodeset->nodeNr; i++) {
			num = xmlNodeListGetString(doc, nodeset->nodeTab[i]->xmlChildrenNode, 1);
			xSize = (int) atof((char*)num);
			xmlFree(num);
		}
		xmlXPathFreeObject (result);
	}
	
	// Get ySize
	result = getNodeSet (doc, xPathTwo);
	if (result) {
		nodeset = result->nodesetval;
		for (int i=0; i < nodeset->nodeNr; i++) {
			num = xmlNodeListGetString(doc, nodeset->nodeTab[i]->xmlChildrenNode, 1);		
			ySize = (int) atof((char*)num);
			xmlFree(num);
		}
		xmlXPathFreeObject (result);
	}
}

// Get the center of the patches from the XML-file
void getPatchPosition(xmlDocPtr doc, int numPatches, double patchPos[][2], xmlChar *xPathOne, xmlChar *xPathTwo) {
	
	// Need to check for empty positions (NULL)!!
	xmlChar *num;
	xmlNodeSetPtr nodeset;
	xmlXPathObjectPtr result;
	
	// Get X-values
	result = getNodeSet (doc, xPathOne);
	if (result) {
		nodeset = result->nodesetval;
		for (int i=0; i < nodeset->nodeNr; i++) {
			num = xmlNodeListGetString(doc, nodeset->nodeTab[i]->xmlChildrenNode, 1);
			patchPos[i][0] = atof((char*)num);
			//printf("x: %f\n", atof((char*)num));
			xmlFree(num);
		}
		xmlXPathFreeObject (result);
	}
	
	// Get Y-values
	result = getNodeSet (doc, xPathTwo);
	if (result) {
		nodeset = result->nodesetval;
		for (int i=0; i < nodeset->nodeNr; i++) {
			num = xmlNodeListGetString(doc, nodeset->nodeTab[i]->xmlChildrenNode, 1);		
			patchPos[i][1] = atof((char*)num);
			//printf("y: %f\n", atof((char*)num));
			xmlFree(num);
		}
		xmlXPathFreeObject (result);
	}
}


// Set the center of the patches
void setPatchData(xmlDocPtr doc, int numPatches, CvPoint patchPos[], int upsideDown,
				  CvPoint dst_corners[4], int scale) {
	
	// Need to check for empty positions (NULL)!!
	xmlNodeSetPtr nodeset;
	xmlXPathObjectPtr result;
	xmlChar *xPath;
	
	// Buffer
	// String instead of char in mac version!
	string temp = "";
	
	//Itoa removed for new function convertInt in Mac-version!! Also added c_str() for xmlNodeSetContent!
	
	// Set X-values
	xPath = (xmlChar*) "/imageData/patchData/patch/center/X";
	result = getNodeSet (doc, xPath);
	nodeset = result->nodesetval;
	if (result) {
		nodeset = result->nodesetval;
		for (int i=0; i < numPatches; i++) {
			temp = convertInt(patchPos[i].x);
			xmlNodeSetContent(nodeset->nodeTab[i], (xmlChar*) temp.c_str());	
		}
		xmlXPathFreeObject(result);
	}
	
	// Set Y-values
	xPath = (xmlChar*) "/imageData/patchData/patch/center/Y";
	result = getNodeSet (doc, xPath);
	if (result) {
		nodeset = result->nodesetval;
		for (int i=0; i < numPatches; i++) {
			temp = convertInt(patchPos[i].y);
			xmlNodeSetContent(nodeset->nodeTab[i], (xmlChar*) temp.c_str());	
		}
		xmlXPathFreeObject(result);
	}
	
	// Set number of patches
	xPath = (xmlChar*) "/imageData/generalData/numberOfPatches";
	result = getNodeSet (doc, xPath);
	if (result) {
		nodeset = result->nodesetval;
		temp = convertInt(numPatches);
		xmlNodeSetContent(nodeset->nodeTab[0], (xmlChar*) temp.c_str());
		xmlXPathFreeObject(result);
	}
	
	// Set orientation of target
	xPath = (xmlChar*) "/imageData/generalData/targetUpsideDown";
	result = getNodeSet (doc, xPath);
	if (result) {
		nodeset = result->nodesetval;
		temp = convertInt(upsideDown);
		xmlNodeSetContent(nodeset->nodeTab[0], (xmlChar*) temp.c_str());
		xmlXPathFreeObject(result);
	}
	
	// Set corners of the target (X-values)
	xPath = (xmlChar*) "/imageData/generalData/positionOfTarget/corner/X";
	result = getNodeSet (doc, xPath);
	nodeset = result->nodesetval;
	int size = (nodeset) ? nodeset->nodeNr : 0;
	if (result) {
		nodeset = result->nodesetval;
		for (int i=0; i < size; i++) {
			temp = convertInt(scale*dst_corners[i].x);
			xmlNodeSetContent(nodeset->nodeTab[i], (xmlChar*) temp.c_str());	
		}
		xmlXPathFreeObject(result);
	}
	// Set corners of the target (Y-values)
	xPath = (xmlChar*) "/imageData/generalData/positionOfTarget/corner/Y";
	result = getNodeSet (doc, xPath);
	nodeset = result->nodesetval;
	if (result) {
		nodeset = result->nodesetval;
		for (int i=0; i < size; i++) {
			temp = convertInt(scale*dst_corners[i].y);
			xmlNodeSetContent(nodeset->nodeTab[i], (xmlChar*) temp.c_str());	
		}
		xmlXPathFreeObject(result);
	}
}

double compareSURFDescriptors(const float* d1, const float* d2, double best,
							  int length )
{
    double total_cost = 0;
    assert( length % 4 == 0 );
    for( int i = 0; i < length; i += 4 )
    {
        double t0 = d1[i] - d2[i];
        double t1 = d1[i+1] - d2[i+1];
        double t2 = d1[i+2] - d2[i+2];
        double t3 = d1[i+3] - d2[i+3];
        total_cost += t0*t0 + t1*t1 + t2*t2 + t3*t3;
        if( total_cost > best )
            break;
    }
    return total_cost;
}

void flannFindPairs(const CvSeq*, const CvSeq* objectDescriptors,
					const CvSeq*, const CvSeq* imageDescriptors,
					vector<int>& ptpairs )
{
	int length = (int)(objectDescriptors->elem_size/sizeof(float));
	
    cv::Mat m_object(objectDescriptors->total, length, CV_32F);
	cv::Mat m_image(imageDescriptors->total, length, CV_32F);
	
	
	// copy descriptors
    CvSeqReader obj_reader;
	float* obj_ptr = m_object.ptr<float>(0);
    cvStartReadSeq( objectDescriptors, &obj_reader );
    for(int i = 0; i < objectDescriptors->total; i++ )
    {
        const float* descriptor = (const float*)obj_reader.ptr;
        CV_NEXT_SEQ_ELEM( obj_reader.seq->elem_size, obj_reader );
        memcpy(obj_ptr, descriptor, length*sizeof(float));
        obj_ptr += length;
    }
    CvSeqReader img_reader;
	float* img_ptr = m_image.ptr<float>(0);
    cvStartReadSeq( imageDescriptors, &img_reader );
    for(int i = 0; i < imageDescriptors->total; i++ )
    {
        const float* descriptor = (const float*)img_reader.ptr;
        CV_NEXT_SEQ_ELEM( img_reader.seq->elem_size, img_reader );
        memcpy(img_ptr, descriptor, length*sizeof(float));
        img_ptr += length;
    }
	
    // Find nearest neighbors using FLANN
    cv::Mat m_indices(objectDescriptors->total, 2, CV_32S);
    cv::Mat m_dists(objectDescriptors->total, 2, CV_32F);
	// Using 4 randomized kdtrees
    cv::flann::Index flann_index(m_image, cv::flann::KDTreeIndexParams(4)); 
	// Maximum number of leafs checked
    flann_index.knnSearch(m_object, m_indices, m_dists, 2, cv::flann::SearchParams(64) ); 
	
    int* indices_ptr = m_indices.ptr<int>(0);
    float* dists_ptr = m_dists.ptr<float>(0);
    for (int i=0;i<m_indices.rows;++i) {
    	if (dists_ptr[2*i]<0.6*dists_ptr[2*i+1]) {
    		ptpairs.push_back(i);
    		ptpairs.push_back(indices_ptr[2*i]);
    	}
    }
}

/* A rough implementation for object location */
int locatePlanarObject(const CvSeq* objectKeypoints, const CvSeq* objectDescriptors,
					   const CvSeq* imageKeypoints, const CvSeq* imageDescriptors,
					   const CvPoint src_corners[4], CvPoint dst_corners[4] )
{
    double h[9];
    CvMat _h = cvMat(3, 3, CV_64F, h);
    vector<int> ptpairs;
    vector<CvPoint2D32f> pt1, pt2;
    CvMat _pt1, _pt2;
    int i, n;
	
	flannFindPairs( objectKeypoints, objectDescriptors, imageKeypoints, imageDescriptors, ptpairs );
	
    n = ptpairs.size()/2;
    if( n < 4 )
        return 0;
	
    pt1.resize(n);
    pt2.resize(n);
    for( i = 0; i < n; i++ )
    {
        pt1[i] = ((CvSURFPoint*)cvGetSeqElem(objectKeypoints,ptpairs[i*2]))->pt;
        pt2[i] = ((CvSURFPoint*)cvGetSeqElem(imageKeypoints,ptpairs[i*2+1]))->pt;
    }
	
    _pt1 = cvMat(1, n, CV_32FC2, &pt1[0] );
    _pt2 = cvMat(1, n, CV_32FC2, &pt2[0] );
    if( !cvFindHomography( &_pt1, &_pt2, &_h, CV_RANSAC, 5 ))
        return 0;
	
    for( i = 0; i < 4; i++ ) {
        double x = src_corners[i].x, y = src_corners[i].y;
        double Z = 1./(h[6]*x + h[7]*y + h[8]);
        double X = (h[0]*x + h[1]*y + h[2])*Z;
        double Y = (h[3]*x + h[4]*y + h[5])*Z;
        dst_corners[i] = cvPoint(cvRound(X), cvRound(Y));
    }
	
    return 1;
}

// Check the orientation of the target. If it is
// upside down, change the order of the corners.
int orientation(CvPoint dst_corners[4]){
	
	int upsideDown = 0;
	
	if (dst_corners[0].x > dst_corners[1].x) {
		
		upsideDown = 1;
		printf("Target is upsidedown\n");
		
		int tempX = dst_corners[2].x;
		int tempY = dst_corners[2].y;
		dst_corners[2].x = dst_corners[0].x;
		dst_corners[2].y = dst_corners[0].y;
		dst_corners[0].x = tempX;
		dst_corners[0].y = tempY;
		
		tempX = dst_corners[3].x;
		tempY = dst_corners[3].y;
		dst_corners[3].x = dst_corners[1].x;
		dst_corners[3].y = dst_corners[1].y;
		dst_corners[1].x = tempX;
		dst_corners[1].y = tempY;
	}
	else {
		printf("Target is oriented normally\n");
	}
	
	return upsideDown;
}

// Return the average color value of the image around the specified point.
// sampleSize determines the size of the stencil where
// size = (1+sampleSize*2)*(1+sampleSize*2)
// Works with b/w etc. Limited to 512 patches, should use dynamic allocatio to fix it
// Note: do not use until yet get correct lab colors. Kept as a reference.
int measureColorLab(IplImage* image, double colorValues[3], CvPoint middle, int sampleSize)
{
	
	//IplImage* dst;
	image = cvLoadImage( "C:/Documents and Settings/henjoh/workspace/Find/scene.png", CV_LOAD_IMAGE_COLOR );
	// Convert to L*a*b*
	//cvCvtColor(image,image,CV_BGR2Lab);
	// Add check to ensure that both row and cols > 0
	if (middle.x - sampleSize < 0 || middle.y - sampleSize < 0)
		return -1;
	
	double red=0, green=0, blue=0;
	int chans = image->nChannels;
	
	// Compute color values
	uchar* ptr = (uchar*) image->imageData;
	for (int row = middle.y-(int)sampleSize; row <= middle.y + (int)sampleSize; row++)
	{
		for (int cols = middle.x-(int)sampleSize; cols <= middle.x + (int)sampleSize; cols++)
		{
			red += ((uchar*)(ptr + row*image->widthStep))[sizeof(uchar)*cols*chans+2];
			green += ((uchar*)(ptr + row*image->widthStep))[sizeof(uchar)*cols*chans+1];
			blue += ((uchar*)(ptr + row*image->widthStep))[sizeof(uchar)*cols*chans];
		}
	}
	int samp = (int) pow((double)(1 + sampleSize*2), 2);
	colorValues[0] = (double)(blue/samp)*0.3921;
	colorValues[1] = (double)(green/samp)-128;
	colorValues[2] = (double)(red/samp)-128;
	//printf("X: %d, Y: %d, Red: %f, Green %f, Blue: %f\n", middle.x, middle.y, colorValues[0], colorValues[1], colorValues[2]);
	//cvCvtColor(image,image,CV_Lab2BGR);
	return 0;
}


// Return the average color value of the image around the specified point.
// sampleSize determines the size of the stencil where
// size = (1+sampleSize*2)*(1+sampleSize*2)
// Works with b/w etc. Limited to 12 patches, sshould use dynamic allocatio to fix it.
// Note: Not used.
int measureColorBGR(IplImage* image, int colorValues[3], CvPoint middle, int sampleSize)
{
	
	// Add check to ensure that both row and cols > 0
	if (middle.x - sampleSize < 0 || middle.y - sampleSize < 0)
		return -1;
	
	int red=0, green=0, blue=0;
	int chans = image->nChannels;
	
	// Compute color values
	uchar* ptr = (uchar*) image->imageData;
	for (int row = middle.y-(int)sampleSize; row <= middle.y + (int)sampleSize; row++)
	{
		for (int cols = middle.x-(int)sampleSize; cols <= middle.x + (int)sampleSize; cols++)
		{
			red += ((uchar*)(ptr + row*image->widthStep))[sizeof(uchar)*cols*chans+2];
			green += ((uchar*)(ptr + row*image->widthStep))[sizeof(uchar)*cols*chans+1];
			blue += ((uchar*)(ptr + row*image->widthStep))[sizeof(uchar)*cols*chans];
		}
	}
	int samp = (int) pow((double)(1 + sampleSize*2), 2);
	// Compute the average value
	colorValues[0] = red/samp; colorValues[1] = green/samp; colorValues[2] = blue/samp;
	printf("Samp: %d, Red: %d, Green %d, Blue: %d\n", samp, colorValues[0], colorValues[1], colorValues[2]);
	
	return 0;
}

int match(IplImage* image, IplImage* object, CvPoint dst_corners[4])
{
	CvSeq *objectKeypoints = 0, *objectDescriptors = 0;
	CvSeq *imageKeypoints = 0, *imageDescriptors = 0;
	
	CvMemStorage* storage = cvCreateMemStorage(0);
	CvSURFParams params = cvSURFParams(500, 1);
	
	// Extract descriptors for the target
	cvExtractSURF( object, 0, &objectKeypoints, &objectDescriptors, storage, params );
	printf("Object Descriptors: %d\n", objectDescriptors->total);
	
	// Extract descriptors for the image (book etc.)
	cvExtractSURF( image, 0, &imageKeypoints, &imageDescriptors, storage, params );
	printf("Image Descriptors: %d\n", imageDescriptors->total);
	
	CvPoint src_corners[4] = {{0,0}, {object->width,0}, {object->width, object->height}, {0, object->height}};
	
	if(!locatePlanarObject( objectKeypoints, objectDescriptors, imageKeypoints,
						   imageDescriptors, src_corners, dst_corners ))
	{
		printf("Unable to find the target in the supplied image\n");
		return -1;
	}
	else
		return 1;
}

// Compute the center postion of the patches
void computePatches(CvPoint dst_corners[4], double patchPos[][2], CvPoint pos[], int numPatches, 
					double angle, int scale, int upsideDown, int xSize, int ySize, IplImage* image) {
	
	double c,d,e,f,g,h,j,k,d_half;
	
	for (int i = 0; i < numPatches; i++) {
		if (!upsideDown)
			d_half=patchPos[i][1];
		else
			d_half=1-patchPos[i][1];
		
		c = sqrt(pow((double)dst_corners[1].x*scale - (double)dst_corners[0].x*scale,2) +
				 pow((double)dst_corners[1].y*scale - (double)dst_corners[0].y*scale,2));
		d = c*((double)ySize/xSize);
		
		// Patches might not be evenly centered with regard to the length. If upsidedown, the center of
		// the patches is found by switching the order of the positions (i.e. starting by last entry
		// in the array).
		if (!upsideDown)
			e = c * patchPos[i][0];
		else 
			e = c * patchPos[(numPatches-1)-i][0];
		
		f = d*d_half*tan(angle);
		g = e-f;
		h = g*cos(angle);
		j = h*tan(angle);
		
		// Patches might not be evenly centered with regard to the height. If upsidedown, the center of
		// the patches is found by inverting each value in the array (1-value).
		if (!upsideDown)
			k = (patchPos[i][1]*d)/cos(angle);
		else 
			k = ((1-patchPos[i][1])*d)/cos(angle);
		
		pos[i].x = (int) (h + scale*dst_corners[0].x);
		pos[i].y = (int) (j + k + scale*dst_corners[0].y);
		
		// Show computed patch centers and the outline of the target
		cvLine(image, cvPoint (scale*dst_corners[0].x, scale*dst_corners[0].y), 
			   cvPoint (scale*dst_corners[1].x, scale*dst_corners[1].y), cvScalar(0,255,255), 12);
		cvLine(image, cvPoint (scale*dst_corners[1].x, scale*dst_corners[1].y), 
			   cvPoint (scale*dst_corners[2].x, scale*dst_corners[2].y), cvScalar(0,255,255), 12);
		cvLine(image, cvPoint (scale*dst_corners[2].x, scale*dst_corners[2].y), 
			   cvPoint (scale*dst_corners[3].x, scale*dst_corners[3].y), cvScalar(0,255,255), 12);
		cvLine(image, cvPoint (scale*dst_corners[3].x, scale*dst_corners[3].y), 
			   cvPoint (scale*dst_corners[0].x, scale*dst_corners[0].y), cvScalar(0,255,255), 12);
		cvCircle(image, cvPoint(pos[i].x, pos[i].y), 7, cvScalar(0,255,255), 15);
		
	}
}


int main(int argc, char** argv){
	
	printf("*********************************\n");
	printf("Find output:\n");
	printf("Num args: %d\n", argc);
	//	char* object_filename = argv[1]; 
	//char* scene_filename = argv[2]; 
	//char* data_filename = argv[3];
	//char* saveTemplate_filename = argv[4];
	//char* save_filename = argv[5];
	
	// const char instead of char in Mac version
	const char* object_filename = argc == 7 ? argv[1] : "/Users/Henrik/Documents/Find_test/linjal.png";
	const char* scene_filename = argc == 7 ? argv[2] : "/Users/Henrik/Documents/Find_test/scene4.png";
	const char* data_filename = argc == 7 ? argv[3] : "/Users/Henrik/Documents/Find_test/testTargetData.xml";
	const char* saveTemplate_filename = argc == 7 ? argv[4] : "/Users/Henrik/Documents/Find_test/imageDataTemplate.xml";
	const char* save_filename = argc == 7 ? argv[5] : "/Users/Henrik/Documents/Find_test/imageData.xml";
	const char* output = argc == 7 ? argv[6] : "/Users/Henrik//Documents/Find_test.png";
	
	int len = strlen(object_filename);
	printf("Target file: ");
	for (int i = 0; i < len; i++) {
		printf("%c", object_filename[i]);
	}
	
	len = strlen(scene_filename);
	printf("\nImage file: ");
	for (int i = 0; i < len; i++) {
		printf("%c", scene_filename[i]);
	}
	
	len = strlen(data_filename);
	printf("\nTarget data: ");
	for (int i = 0; i < len; i++) {
		printf("%c", data_filename[i]);
	}
	
	len = strlen(saveTemplate_filename);
	printf("\nImage template: ");
	for (int i = 0; i < len; i++) {
		printf("%c", saveTemplate_filename[i]);
	}
	
	len = strlen(save_filename);
	printf("\nImage data: ");
	for (int i = 0; i < len; i++) {
		printf("%c", save_filename[i]);
	}
	
	len = strlen(output);
	printf("\nOutput file: ");
	for (int i = 0; i < len; i++) {
		printf("%c", output[i]);
	}
	printf("\n");
	
    //object_filename = "C:/Programmering/Matchning/linjal.png";
	//scene_filename ="C:/Programmering/Matchning/sceneLAB.tif";
	
	// Get target data from file
	// In Mac-version const_cast <char*> (variable)
	xmlDocPtr doc = getDoc(const_cast <char*> (data_filename));
	xmlChar *xPathOne, *xPathTwo;
	
	
    IplImage* image = cvLoadImage( scene_filename, CV_LOAD_IMAGE_GRAYSCALE); 
	if (image == NULL) {
		printf("Error loading target image");
		return -1;
	}
	
	IplImage* object = cvLoadImage( object_filename, CV_LOAD_IMAGE_GRAYSCALE ); 
	if (object == NULL) {
		printf("Error loading reference image");
		return -1;
	}
	
	if( !object || !image )
    {
        fprintf( stderr, "Can not load %s and/or %s\n"
				"Usage: find_obj [<object_filename> <scene_filename>]\n",
				object_filename, scene_filename );
        return -1;
    }
	
	// Get the numberOfPatches
	xPathOne = (xmlChar*) "/targetData/generalData/numberOfPatches";
	int numPatches = 0;
	numPatches = getNumberOfPatches(doc, xPathOne);
	printf("Number of Patches: %d\n", numPatches);
	
	double patchPos[512][2];
	
	// Get the center of the patches
	xPathOne = (xmlChar*) "/targetData/patches/patch/relativeCenter/X";
	xPathTwo = (xmlChar*) "/targetData/patches/patch/relativeCenter/Y";
	getPatchPosition(doc, numPatches, patchPos, xPathOne, xPathTwo);
	
	// Get the size of the target
	int xSize = 0, ySize = 0;
	xPathOne = (xmlChar*) "/targetData/generalData/size/sizeInPixels/X";
	xPathTwo = (xmlChar*) "/targetData/generalData/size/sizeInPixels/Y";
	getTargetSize(doc, xSize, ySize, xPathOne, xPathTwo);
	
	// Reduce the size of the images to speed up computations
	int numberOfTimesReduced = 0;
	int treshold= 10000;
	while (object->width > treshold || object->height > treshold) {
		object=doPyrDown(object, CV_GAUSSIAN_5x5);
		image=doPyrDown(image, CV_GAUSSIAN_5x5);
		numberOfTimesReduced++;
	}
	while (image->width>treshold || image->height > treshold)  {
		image=doPyrDown(image, CV_GAUSSIAN_5x5);
		object=doPyrDown(object, CV_GAUSSIAN_5x5);
		numberOfTimesReduced++;
	}
	printf("The size of the images were reduced %d times.\n", numberOfTimesReduced);
	int scale = (int) pow((double)numberOfTimesReduced,2);
	if (numberOfTimesReduced == 0)
		scale = 1;
	else if (numberOfTimesReduced == 1)
		scale = 2;
	
	// Perform the matching
	CvPoint dst_corners[4];
	if (match(image, object, dst_corners) == -1)
		return -1;
	
	// Check if the target is upsidedown
	int upsideDown = orientation(dst_corners);
	
	// Compute the angle of the target
	double angle = (atan ((double)(dst_corners[1].y-dst_corners[0].y)/(double)(dst_corners[1].x-dst_corners[0].x)));
	printf("Angle (r): %f, angle: %f\n", angle, 360*angle/(2*3.1415));
	
	// Compute the center position of the patches in the image
	CvPoint pos[512];
	// Remove next line in release!
	image = cvLoadImage(scene_filename, CV_LOAD_IMAGE_COLOR );
	computePatches(dst_corners, patchPos, pos, numPatches, angle, scale, upsideDown, xSize, ySize, image);
	
	// Save the data in the XML-file
	// In Mac-version const_cast <char*> (variable)
	doc = getDoc(const_cast <char*> (saveTemplate_filename));
	setPatchData(doc, numPatches, pos, upsideDown, dst_corners, scale);
	xmlSaveFileEnc(save_filename, doc, "UTF-8");
	
	// Just for now...
	image=doPyrDown(image, CV_GAUSSIAN_5x5);
	image=doPyrDown(image, CV_GAUSSIAN_5x5);
	image=doPyrDown(image, CV_GAUSSIAN_5x5);
	
	//cvNamedWindow("Matched patches", CV_WINDOW_AUTOSIZE);
	// cvShowImage( "Matched patches", image);
	// Need to have access pÂ path!
	if(!cvSaveImage(output, image)) 
		printf("Could not save output image\n");
	
	
	//	cvWaitKey(0);
	cvReleaseImage(&image);
	cvReleaseImage(&object);
    cvDestroyWindow("Matched patches");
	xmlFreeDoc(doc);
	xmlCleanupParser();
	
	printf("End Find\n");
	printf("*********************************\n");
    return 1;
}
