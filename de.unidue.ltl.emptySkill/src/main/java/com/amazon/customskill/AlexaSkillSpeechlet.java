/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at
        http://aws.amazon.com/apache2.0/
    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.amazon.customskill;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.customskill.AlexaSkillSpeechlet.UserIntent;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SsmlOutputSpeech;

import java.lang.Math;

/*
 * This class is the actual skill. Here you receive the input and have to produce the speech output. 
 */
public class AlexaSkillSpeechlet
implements SpeechletV2
{
	static Logger logger = LoggerFactory.getLogger(AlexaSkillSpeechlet.class);

	//Idee: Variablen die wir auch in Dialogos benutzt haben z.B. e1,e2,e3
	static boolean tabuwortUsed;
	static String getTabuwort = "";

    //Was User sagt 
	public static String userRequest;

	//Muss noch angepasst werden auf unser Modell
	//Möchten Sie das Spiel anfangen oder weiterspielen? - Ja - selectTabuwort
	//Nein - Aufwiedersehen, danke fürs spielen
	static enum RecognitionState {Erklaerung, JaNein};
	RecognitionState recState;

	//Was User gerade gesagt hat - semantictags aus DialogOS - e1,e2,e3
	static enum UserIntent{UserErklaerung, UserNenntTabuwort, Ja, Nein}; //UserErklärung und UserNenntTabuwort = Pattern anlegen
	UserIntent ourUserIntent;

	//Was System sagen kann
	Map <String, String> utterances;


	//baut systemaeußerung zsm
	String buildString(String msg, String replacement1, String replacement2) {
		return msg.replace("{replacement}", replacement1).replace("{replacement2}", replacement2);
	}

	//ließt am Anfang alle systemaeußerungen aus datei ein
	Map<String, String> readSystemUtterances() {
		Map<String, String> utterances = new HashMap<String, String>();
		try {
			for(String line : IOUtils.readLines(this.getClass().getClassLoader().getResourceAsStream("utterances.txt"))){
				if (line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("=");
				String key = parts[0].trim();
				String utterance = parts[1].trim();
				utterances.put(key, utterance);
			}
			logger.info("Read " + utterances.keySet().size() + "utterances");
		} catch (IOException e) {
			logger.info("Could not read utterances: "+e.getMessage());
			System.err.println("Could not read utterances:"+e.getMessage());
		}
		return utterances;
	}

	//datenbank woraus tabuwort gezogen wird
	static String DBName = "Tabu.db";
	private static Connection con = null;

	@Override
    public void onSessionStarted (SpeechletRequestEnvelope <SessionStartedRequest> requestEnvelope)
    {
		logger.info ("Alexa, ich möchte Tabu spielen.");
		utterances = readSystemUtterances();
    }

    //Wir starten dialog
	//hole erstes Tabuwort aus der datenbank
	//Ließ die begüßung vor und frage ob user regeln kennt oder nicht
	//wenn (if) user regeln kennt, starte spiel, (else) ansonten erkläre regeln  
	// wir wollen dann antwort erkennen vom user
	@Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope <LaunchRequest> requestEnvelope)
    {
	   logger.info("onLaunch");
	   
	   recState = RecognitionState.Erklaerung;
	   selectTabuwort(); //daran soll tabuwort abgerufen werden

       return askUserResponse (utterances.get("welcomeMsg")+" "+getTabuwort);
       
    }
    

    
    //datenbankabfrage des tabuworts
    private void selectTabuwort() {
    	logger.info("Es wird auf die Datenbank zugegriffen");
    	try {
    		con = DBConnect.getConnection();  
    		Statement stmt = con.createStatement();
    		//int randomId = (int) (Math.random() * 10);
    		ResultSet rs = stmt
    					.executeQuery("SELECT * FROM Tabu_Woerter WHERE WortID=1" + "");
    		getTabuwort = rs.getString("Tabuwort");
    		logger.info("Extracts random word from database " + getTabuwort);
    		con.close();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    } 

    // ++++++++++++ BIS HIER ALLES RICHTIG, AB HIER WEITERMACHEN +++++++++++++++++

    //hier wird gespeichert was der User sagt.
    //String wird in Userrequest gespeichert
    //je nach cognition State reagiert das system unterschiedlich
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope)
	{
		IntentRequest request = requestEnvelope.getRequest();
		Intent intent = request.getIntent();
		userRequest = intent.getSlot("anything").getValue();
		logger.info("Received following text: [" + userRequest + "]");
		logger.info("recState is [" + recState + "]");
		logger.info("Erklaerung wird erkannt");
		SpeechletResponse resp = null;
		
		switch (recState) {
		case Erklaerung: 
			resp = evaluateErklaerung(userRequest); break;
		case JaNein: 
			resp = evaluateJaNein(userRequest); 
			recState = RecognitionState.Erklaerung; break;
		default: 
			resp = tellUserAndFinish("Erkannter Text: " + userRequest);
		}   
		return resp;
	}
		

    //if Alexa tabuwort errät, dann fragen ob nochmal spielen
    //else if User nennt tabuwort, dann Stoppe und system.out.println(du hast tabuwort gesagt);
    // else 
    private SpeechletResponse evaluateErklaerung(String userRequest) {
    	
    	SpeechletResponse res = null;
    	switch (ourUserIntent) {
    	case UserNenntTabuwort:
    	{
    		if (tabuwortUsed = true) {
    			res = askUserResponse(utterances.get("tabuMsg"));
    		}
    	
    	} default: {
    		if(ourUserIntent.equals(UserIntent.UserErklaerung))
    			 {
    			logger.info("User answer ="+ ourUserIntent.name().toLowerCase());
    		}
    		if (ourUserIntent.name().toLowerCase().equals(getTabuwort)) {
    			
				logger.info("");
    		    res = askUserResponse(utterances.get("Deine Erklaerung war richtig?"));
    	    } 
    	}
    	} return res;
    }
    
    // Möchtest du weiterspielen?
    // Ja: wähle erneut ein tabuwort selectTabuwort();
    // Nein: GoodbyeMsg
	private SpeechletResponse evaluateJaNein(String userRequest) {
	
	
		SpeechletResponse res = null;
		res = askUserResponse(utterances.get("weiterspielenMsg"));
		
		switch (ourUserIntent) {
		case Ja: {
			selectTabuwort();
		    break;
		} case  Nein: {
			res = tellUserAndFinish(utterances.get("goodbyeMsg")); break;
		} default: {
			res = askUserResponse(utterances.get(""));
		}
		}
		return res; 
	
	}

	//Gucken ob User weitermachen will
    //Wenn ja, dann stelle Tabuwort
    //Wenn nein, dann beende das Spiel
    private SpeechletResponse UserErklaerung(String userRequest) {
		SpeechletResponse res = null;
		
		switch (ourUserIntent) {
		case UserErklaerung: {
			selectTabuwort();
			res = askUserResponse(getTabuwort); break;
		} case UserNenntTabuwort: {
			res = tellUserAndFinish(utterances.get("goodbye")); break;
		} default: {
			res = askUserResponse(utterances.get(""));
		}
		}
		return res; 

    }
    
    private SpeechletResponse UserNenntTabuwort(String userRequest) {
    	return null;
    }

    //THIS SHOULD JUST BE THE SAME AS GIVEN -- ALEXASKILLSPEECHLET.JAVA FROM WWM
    //Useräußerung vorhanden
    // und LIste mit Tabuwörter
    // If useräußerung contains tabuwort = true else return false
    //public, boolean, parameter (string), liste mit 
    
    public boolean containsTabuwort(String utterance, List<String> tabuwords) {
    	
    	for (String tabuword : tabuwords) {
    		if (utterance.contains(tabuword)) {
    			return true;
    		} 
    	}
    	return false;
    	//anstelle des pattern matching: matcht meine Useräußerung den Tabuwörtern?
    }
    
   

	//tell the user smth or Alexa ends session after a 'tell'
    private SpeechletResponse tellUserAndFinish(String text)
	{
		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(text);

		return SpeechletResponse.newTellResponse(speech);
	}



	private SpeechletResponse responseWithFlavour(String text, int i) {

		SsmlOutputSpeech speech = new SsmlOutputSpeech();
		switch(i){ 
		case 0: 
			speech.setSsml("<speak><amazon:effect name=\"whispered\">" + text + "</amazon:effect></speak>");
			break; 
		case 1: 
			speech.setSsml("<speak><emphasis level=\"strong\">" + text + "</emphasis></speak>");
			break; 
		case 2: 
			String half1=text.split(" ")[0];
			String[] rest = Arrays.copyOfRange(text.split(" "), 1, text.split(" ").length);
			speech.setSsml("<speak>"+half1+"<break time=\"3s\"/>"+ StringUtils.join(rest," ") + "</speak>");
			break; 
		case 3: 
			String firstNoun="erstes Wort buchstabiert";
			String firstN=text.split(" ")[3];
			speech.setSsml("<speak>"+firstNoun+ "<say-as interpret-as=\"spell-out\">"+firstN+"</say-as>"+"</speak>");
			break; 
		case 4: 
			speech.setSsml("<speak><audio src='soundbank://soundlibrary/transportation/amzn_sfx_airplane_takeoff_whoosh_01'/></speak>");
			break;
		default: 
			speech.setSsml("<speak><amazon:effect name=\"whispered\">" + text + "</amazon:effect></speak>");
		} 

		return SpeechletResponse.newTellResponse(speech);
	}


	@Override
	public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope){
		logger.info("Das Tabuspiel ist zuende. Bis zum nächsten Mal");
	}

	/**
	 * Tell the user something - the Alexa session ends after a 'tell'
	 */
	private SpeechletResponse response(String text)
	{
		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(text);

		return SpeechletResponse.newTellResponse(speech);
	}

	/**
	 * A response to the original input - the session stays alive after an ask request was send.
	 *  have a look on https://developer.amazon.com/de/docs/custom-skills/speech-synthesis-markup-language-ssml-reference.html
	 * @param text
	 * @return
	 */
	private SpeechletResponse askUserResponse(String text)
	{
		SsmlOutputSpeech speech = new SsmlOutputSpeech();
		speech.setSsml("<speak>" + text + "</speak>");

		// reprompt after 8 seconds
		SsmlOutputSpeech repromptSpeech = new SsmlOutputSpeech();
		repromptSpeech.setSsml("<speak><emphasis level=\"strong\">Hey!</emphasis> Bist du noch da?</speak>");

		Reprompt rep = new Reprompt();
		rep.setOutputSpeech(repromptSpeech);

		return SpeechletResponse.newAskResponse(speech, rep);
	}


}

