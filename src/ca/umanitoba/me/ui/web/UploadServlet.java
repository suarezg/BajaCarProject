// ========================================================================
// Copyright (c) 2009-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================


package ca.umanitoba.me.ui.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import ca.umanitoba.me.car.filemanager.VolumeGetter;
import ca.umanitoba.me.ui.BajaUserInterface;
import ca.umanitoba.me.ui.BajaUserInterface.UIEvent;

@SuppressWarnings("serial")
public class UploadServlet extends HttpServlet
{
	private final static Logger LOGGER = Logger.getLogger(UploadServlet.class.getCanonicalName());

	private BajaUserInterface ui = BajaWebServerUI.getTheInstance();

	@Override
	public void init(){
		// TODO Auto-generated method stub
		System.out.println("init()");
		try {
			super.init();
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void init(ServletConfig config) {
		// TODO Auto-generated method stub
		System.out.println("init(" + config + ")");
		try {
			super.init(config);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public UploadServlet()
	{
		super();
		System.out.println("construct uploadServlet");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		System.out.println("do post");

		// response.setContentType("text/html;charset=UTF-8");

		//	    Collection<Part> reqParts;
		//		try {
		//			reqParts = request.getParts();
		//			System.out.println("parts: ");
		//		    for (Part part : reqParts) 
		//		    {
		//		    	System.out.println("\t" + part.getName());
		//		    }
		//		} catch (ServletException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}


		// Create path components to save the file

		try {
			File path = VolumeGetter.getDirectoryOnDrive("BAJA_DISK", "profiles");
			final Part filePart = request.getPart("fileToUpload");
			final String fileName = getFileName(filePart);

			OutputStream out = null;
			InputStream filecontent = null;
			//final PrintWriter writer = response.getWriter();

			try {
				out = new FileOutputStream(new File(path, fileName));
				filecontent = filePart.getInputStream();

				int read = 0;
				final byte[] bytes = new byte[1024];

				while ((read = filecontent.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				LOGGER.log(Level.INFO, "File{0}being uploaded to {1}", 
						new Object[]{fileName, path});
			} catch (FileNotFoundException fne) {
				ui.notifyUI(UIEvent.COULD_NOT_FIND_OUTPUT_VOLUME);

				LOGGER.log(Level.SEVERE, "Problems during file upload. Error: {0}", 
						new Object[]{fne.getMessage()});
			} finally {
				if (out != null) {
					out.close();
				}
				if (filecontent != null) {
					filecontent.close();
				}
			}
		} catch (FileNotFoundException e) 
		{
			ui.notifyUI(UIEvent.COULD_NOT_FIND_OUTPUT_VOLUME);
		}

	}

	private String getFileName(final Part part) {
		final String partHeader = part.getHeader("content-disposition");
		LOGGER.log(Level.INFO, "Part Header = {0}", partHeader);
		for (String content : part.getHeader("content-disposition").split(";")) {
			if (content.trim().startsWith("filename")) 
			{
				String nameFull = content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
				String[] filePartsWindows = nameFull.split("\\\\");
				String[] filePartsUnix = nameFull.split("/");
				
				// check to see if the file came from a Unix system
				String name;
				if (filePartsUnix.length > filePartsWindows.length)
				{
					name = filePartsUnix[filePartsUnix.length - 1];
				} else
				{
					name = filePartsWindows[filePartsWindows.length - 1];
				}
				
				LOGGER.log(Level.INFO, "Filename = {0}", name );
				return name;
			}
		}
		return null;
	}


	//	@Override
	//	protected void doPost(HttpServletRequest req , HttpServletResponse response) throws IOException, ServletException 
	//	{
	//		StringBuffer buff = new StringBuffer();
	//		
	////		Collection<Part> reqParts = req.getParts();
	////		System.out.println("parts: ");
	////		for (Part part : reqParts) 
	////		{
	////			System.out.println("\t" + part.getName());
	////		}
	//		
	//		
	//		System.out.println("reader:");
	//		BufferedReader reader = req.getReader();
	//	    String str = "";
	//	    while ((str = reader.readLine()) != null)
	//	    {
	//	        System.out.println(str);
	//	    }
	//	    
	//	    System.out.println("// End reader");
	//		
	//		System.out.println("content-len: " + req.getHeader("Content-Length"));
	//		System.out.println("Content-type: " + req.getContentType());
	//		
	//		File file1 = (File) req.getAttribute( "fileToUpload" );	 
	//
	//		if( file1 == null || !file1.exists() )
	//		{
	//			buff.append( "File does not exist" );
	//		}
	//		else if( file1.isDirectory())
	//		{
	//			buff.append( "File is a directory" );
	//		}
	//		else
	//		{
	//			File outputFile = new File( req.getParameter( "fileToUpload" ) );
	//			file1.renameTo( outputFile );
	//			buff.append( "File " + outputFile.getAbsolutePath() + " successfully uploaded." );
	//		}
	//		
	//		System.out.println(buff.toString());
	//		
	//		PrintWriter outp = response.getWriter();
	//
	//        outp.write( "<html>" );
	//
	//        outp.write( "<head><title>FileUpload page</title></head>" );
	//
	//        outp.write( "<body>" );
	//
	//        outp.write( "<h2>" + buff.toString() + "</h2>" );
	//
	//        outp.write( "</body>" );
	//
	//        outp.write( "</html>" );
	//	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		System.out.println("do get");
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println("<h1>" + "hello" + " UploadServlet</h1>");
		response.getWriter().println("session=" + request.getSession(true).getId());
	}
}
