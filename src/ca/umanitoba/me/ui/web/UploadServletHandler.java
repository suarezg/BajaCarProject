/**
 * 
 */
package ca.umanitoba.me.ui.web;

import java.io.IOException;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletHandler;

/**
 * Necessary to allow the UploadServlet to function correctly. Based off of {@linkplain http://dev.eclipse.org/mhonarc/lists/jetty-users/msg03294.html}
 * @author Paul
 *
 */
public class UploadServletHandler extends ServletHandler 
{
	private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement("lol");
	@Override
	public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException 
	{
		System.out.println(this.getClass().getCanonicalName() + ": doHandle(" + request.getMethod() + ")");
		if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
			baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
		}
		super.doHandle(target, baseRequest, request, response);
	}

}
