package pl.edu.agh.kis.florist.model;

import java.util.Map;

public class UnsuccessfulLoginError {

	public UnsuccessfulLoginError(Map<String, String> params) {
		setParams(params);
		init();
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
		init();
	}
	
	private void init() {
		this.message = String.format("Unsuccessful login.");
	}
	
	

	private String message;
	private Map<String, String> params;

}
