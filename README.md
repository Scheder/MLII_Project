MLII Project of Simon Slangen and Peter Raeves
==============================================

This is the readme for the project of the Machine Learning and Inductive
 Inference course of 2014-2015 made by Simon Slang and Peter Raeves.

Folder structure
================

The delivered zip file, named MLII\_Simon\_Slangen\_Peter\_Raeves\_201415.zip,
contains:

	- mlii.jar: The main classifying application
	- report.pdf: The report on the project
	- lib: All used libraries
	- plots: Plots of all training data
	- Project: Files used by the application. This folder should not
		be touched.
	- src: All source files written for the application
	- README.md: This readme file

Usage
=====

Assuming the zip file is unzipped you can run the application by executing the
following commands:

	cd MLII_Simon_Slangen_Peter_Raeves_201415
	./mlii.jar
	
The jar file should always be in the same folder as the "Project"-folder, 
otherwise the program will fail to execute. The application will first
initialize by reading the codebooks into memory. This process will take from 
one to three minutes. After the codebooks are loaded, a window will open with
a single button "Browse...". After clicking the button a file choosing dialog
opens, asking which files you would like to classify. Multiple CSV files can be
selected and output will be provided in the command line.