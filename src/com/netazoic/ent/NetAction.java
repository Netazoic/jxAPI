package com.netazoic.ent;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


public interface NetAction {
	  public void doAction(HttpServletRequest request, HttpServletResponse
	      response, HttpSession session) throws ServletException, IOException, Exception;
	}