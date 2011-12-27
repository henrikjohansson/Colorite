// stdafx.cpp : source file that includes just the standard includes
// find3.pch will be the pre-compiled header
// stdafx.obj will contain the pre-compiled type information

#include "stdafx.h"

// TODO: reference any additional headers you need in STDAFX.H
// and not in this file

/*
 *  Finding a ruler in an image and calculating the localtion of the patches
 *  Based on an OpenCV Implementation of SURF by Liu Liu
 *  Further Information Refer to "SURF: Speed-Up Robust Feature"
 *  Author: Henrik Johansson and Liu Liu
 *  henrik.johansson@kb.se, liuliu.1987+opencv@gmail.com
 *
 *  Prerequisite: An image with the mini-colorchecker. The corners are
 *  counted as in the illustration below. If the ruler is placed upside
 *  down, Corner 1 will be in the lower right.
 *
 *  Corner 1					Corner 2
 *  -------------------------------------
 *  | 0								 20 |
 *  |									|
 *  -------------------------------------
 *  Corner 4					Corner 3
 */

