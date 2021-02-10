package com.amazon.customskill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerTest {
	
	static Logger logger = LoggerFactory.getLogger(AlexaSkillSpeechlet.class);
	
	public static void main(String[] args) {
		logger.info("XXX");
		System.out.println("ENDE");
	}
}
