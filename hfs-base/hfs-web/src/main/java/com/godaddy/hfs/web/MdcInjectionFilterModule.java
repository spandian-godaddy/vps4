package com.godaddy.hfs.web;

import com.google.inject.servlet.ServletModule;

public class MdcInjectionFilterModule extends ServletModule {
	
	@Override
	protected void configureServlets() {
		bind(MdcInjectionFilter.class);
		filter("/*").through(MdcInjectionFilter.class);
	}
}
