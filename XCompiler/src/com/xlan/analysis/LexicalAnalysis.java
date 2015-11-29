package com.xlan.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.xlan.analysis.Token.Type;
import com.xlan.expections.LexicalAnalysisException;

public class LexicalAnalysis{

	private State state;
	private boolean transferredMeaningSign=false;
	private final LinkedList <Token> tokenBuffer=new LinkedList <Token>();
	private StringBuilder readBuffer=null;
	private BufferedReader reader; //read codes
	
	private static final char[] IdentifierRearSign =new char[] {'?','/','-','*','=',';','<','>','+',')','('};
	private static final char[] space =new char[]{' ','\n',(char)13, (char)10, (char)9};

	private static final HashMap<Character, Character> StringTMMap = new HashMap<>();

	static {
		StringTMMap.put('\"', '\"');
		StringTMMap.put('\'', '\'');
		StringTMMap.put('\\', '\\');
		StringTMMap.put('b', '\b');
		StringTMMap.put('f', '\f');
		StringTMMap.put('t', '\t');
		StringTMMap.put('r', '\r');
		StringTMMap.put('n', '\n');
		
	}
	private static enum State{
		Normal,Identifier, Sign, Annotation,String, RegEx, Space;
	}
	
	public LexicalAnalysis(Reader reader){
		this.state=State.Normal;
		this.reader=new BufferedReader(reader);
	}

	public LinkedList <Token> read() throws IOException, LexicalAnalysisException{
		boolean moveCursor= true;
		int next=reader.read();
		while(next!=-1){
			System.out.println("next="+" "+(char)next+" cur state="+state);
			moveCursor=readChar((char)next);
			if(moveCursor) next=reader.read();
		}
		return tokenBuffer;
	}


	private void refreshBuffer(char c){
		readBuffer=new StringBuilder(c);
	}



	private void createToken(Type type) {
		Token token = new Token(type, readBuffer.toString());
		tokenBuffer.addFirst(token);
		readBuffer= null;
	}
	
	private void createToken(Type type,String value) {
		Token token = new Token(type,value);
		tokenBuffer.addFirst(token);
		readBuffer=null;
	}
		

	//Finite State Machine
	//jump between different states
	private boolean readChar(char c) throws LexicalAnalysisException {
		boolean moveCursor=true;
		Type createType=null;
		
		
		if(state==State.Normal){
			//from normal state to jump to other states.
			//System.out.println(" c="+(int)c+" state="+state);
			if(inIdentifierSetButNotRear(c)) {
				state = State.Identifier;
			}
			else if(SignParser.inCharSet(c)) {
				state = State.Sign;
			}
			else if(c == '#') {
				state = State.Annotation;
			}
			else if(c == '\"') {
				state = State.String;
			}
			else if(c == '\'') {
				state = State.RegEx;
			}
			else if(include(space,c)) {  
				//System.out.println("space");
				state = State.Space;
			}
			else if(c == '\n') {
				createType = Type.NewLine;
			}
			else if(c == '\0') {
				createType = Type.EndSymbol;
			}else if(c ==';'){
				createType = Type.EndStatement;
			}else {
				throw new LexicalAnalysisException();
			}
			
			refreshBuffer(c);
		}else if(state==State.Identifier){ //number/keyword/var name
			if(inIdentifierSetButNotRear(c)){
				readBuffer.append(c);
			}else if(include(IdentifierRearSign,c)){
				createType=Type.Identifier;
				readBuffer.append(c);
				state=State.Normal;
			}else{
				//not identifier char
				createType=Type.Identifier;
				state=State.Normal;
				moveCursor=false;
			}

		}else if(state==State.Sign){
			if(SignParser.inCharSet(c)){
				readBuffer.append(c);
			}else{
				List<String> list=SignParser.parse(readBuffer.toString()); //exception throw here
				for(String signStr:list){
					createToken(Type.Sign,signStr);
				}
				createType =null;
				state=State.Normal;
				moveCursor=false;
			}
			
			
		}else if(state==State.Annotation){
			if(c=='\n' || c=='\0'){
				createType=Type.Annotation;
				state=State.Normal;	
				moveCursor=false;				
			}else{
				readBuffer.append(c);
			}
			
		}else if(state==State.String){
			if(c=='\"'){ //ending with "
				createType=Type.String;
				state=State.Normal;
				readBuffer.append(c);
			}else if(c=='\\'){
				transferredMeaningSign=true;
			}else if(transferredMeaningSign){
				char tmpSign=StringTMMap.get(c);
				if(tmpSign==' '){
					throw new LexicalAnalysisException();
				}
				readBuffer.append(tmpSign);
				transferredMeaningSign=false;
			}else if(c=='\n' || c=='\0'){
				throw new LexicalAnalysisException();
			}else{
				readBuffer.append(c);
			}
			
		}else if(state==State.RegEx){
			if(c=='\\'){
				transferredMeaningSign=true;
			}else if(c=='\0'){
				throw new LexicalAnalysisException();
			}else if(transferredMeaningSign){
				if(c!='\'') throw new LexicalAnalysisException();
				readBuffer.append(c);
				transferredMeaningSign=false;
			}else if(c=='\''){
				readBuffer.append(c);
				createType=Type.RegEx;
				state=State.Normal;
			}else{
				readBuffer.append(c);
			}
		}else if(state==State.Space){
			if(c==' '){
				readBuffer.append(c);
			}else{
				createType=Type.Space;
				state=State.Normal;
				moveCursor=false;
			}
		}
		
		//create token
		if(createType!=null) {
			createToken(createType);
		}

		return moveCursor;
	}


	
	//
	private boolean inIdentifierSetButNotRear(char c) {
		return (c >= 'a' & c <= 'z' ) | (c >='A' & c <= 'Z') | (c >= '0' & c <= '9')|| (c == '_');
	}

	private boolean include(char[] range, char c) {
		boolean include = false;
		for(int i=0; i<range.length; ++i) {
			if(range[i] == c) {
				include = true;
				break;
			}
		}
		return include;
	}

}