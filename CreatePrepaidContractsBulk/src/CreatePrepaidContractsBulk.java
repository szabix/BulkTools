import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;


import java.util.Iterator;
import java.util.Scanner;

import com.sbx.inventory.ConfigReader;
import com.sbx.inventory.LogWriter;
import com.sbx.inventory.Mailer;


public class CreatePrepaidContractsBulk {

	private static LogWriter Log = new LogWriter("log/ErrorLog",0);
    private static LogWriter Trace = new LogWriter("trace/traceFile",1);
    private ConfigReader conf = null;

    private int procCounter = 0;
    private int errCounter = 0;
    private int succCounter = 0;
    
    private int numThreads = 5;
    private String inputDir = "input";
    private String outputDir = "output";
    private File files[];
    private BufferedReader inFile = null;
    private BufferedWriter outFile = null;
    private BufferedWriter errFile = null;
    private BufferedWriter linkFile = null;
    private int fCounter = 0;
    private String charSet = "";
    private String contract_prefix = "E-E";
    private int contractNumLen = 6;
    private long contractNum = 0;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
    
    private HashMap<String,StringBuffer> esb_templates = new HashMap<String,StringBuffer>();
    
    private HashSet<Thread> threadList = new HashSet<Thread>();
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Log.write("Starting...");
		Trace.write("Starting...");
		CreatePrepaidContractsBulk kr = new CreatePrepaidContractsBulk();

		if (false) {
			kr.testMailer();
			System.exit(0);
		}

		if (!kr.init()) {
			System.err.println("ABORTING...");
			System.exit(1);
		}
		while (kr.threadsAlive()) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
				Trace.write(e.getMessage());
				Trace.writeStackTrace(e.getStackTrace());
				Log.write(e.getMessage());
			}
		}
		kr.exitTasks();
		Trace.write("EXIT");
		Log.write("Exit.");
	}

	private void exitTasks() {
		try {
			storeContractNum();
			outFile.close();
			errFile.close();
		} catch (Exception e) {
			Trace.write(e.getMessage());
			Trace.writeStackTrace(e.getStackTrace());
			Log.write(e.getMessage());
		}
		Log.write("Processed lines: " + procCounter);
		Log.write("ERROR counter  : " + errCounter);
		Log.write("Success counter: " + succCounter);
	}
	
	private void storeContractNum() {
		try {
			FileWriter fw = new FileWriter(new File("conf/contractNum.txt"));
			fw.write(Long.toString(contractNum));
			fw.close();
		} catch (Exception e) {
			Log.write(e.getMessage());
			Trace.write(e.getMessage());
			Trace.writeStackTrace(e.getStackTrace());
		}
	}
	
	private boolean readContractNum() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("conf/contractNum.txt"))));
			contractNum = Long.parseLong(br.readLine());
			br.close();
			return true;
		} catch (Exception e) {
			Log.write(e.getMessage());
			Trace.write(e.getMessage());
			Trace.writeStackTrace(e.getStackTrace());
		}
		return false;
	}
	
	protected StringBuffer getEsbTemplate(String type) {
		return esb_templates.get(type);
	}


	private void testMailer() {
        Mailer m = new Mailer("north-mail01.internal.vodafone.com");
        m.addAuth("szabolcs.toth@vodafone.com","Dreamms17"); //172.20.244.33
        //Mailer m = new Mailer("172.20.244.33","szabolcs.toth@vodafone.com","Dreamms17"); //172.20.244.33
        //Mailer m = new Mailer("172.20.244.33",null,null);
        try {
            Scanner s = new Scanner(new FileInputStream(new File("conf/email.html")),"UTF-8");
            String mailContent = s.useDelimiter("\\Z").next();
            s.close();
        	
        	if (m.init(true)) {
/*
 * public void sendMail(String[] to,
 *  String[] cc,
 *  String[] bcc,
 *  String subject,
 *  String message,
 *  String charSet,
 *  File[] attachments,
 *  String messageType,
 *  String from,
 *  String fromDesc
 *  ) throws Exception{
 */

        		m.sendMail(new String[]{"szabolcs.toth2@gmail.com"}, 
        				new String[]{"szabolcs.toth@vodafone.com"},
        				null,
        				"Előfizetői szerződés " + ((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date())),
        				mailContent,
        				"utf-8",
        				new File[]{new File("test/Vodafone_kartyas_elofizetoi_szerzodes_703390939.pdf")},
        				"text/html; charset=utf-8",
        				"eszrdeliveryreport@internal.vodafone.com",
        				"Vodafone Elektronikus Szerződés Rendszer");
/*
         		m.sendMail(new String[]{"szabolcs.toth2@gmail.com"}, null,null,"Előfizetői szerződés2 " + ((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date())), mailContent, new File("test/Vodafone_kartyas_elofizetoi_szerzodes_703390939.pdf"), "text/html; charset=utf-8", "szabolcs.toth@vodafone.com");
        		//m.sendMail(new String[]{"szabolcs.toth2@gmail.com"}, null,null,"Előfizetői szerződés3 " + ((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date())), mailContent, new File("test/TEST.tiff"), "text/html; charset=utf-8", "szabolcs.toth@vodafone.com");
        		m.sendMail(new String[]{"szabolcs.toth2@gmail.com"}, new String[]{"szabolcs.toth@vodafone.com"},null,"Előfizetői szerződés4 " + ((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date())), mailContent, new File("conf/conf.ini"), "text/html; charset=utf-8", "szabolcs.toth@vodafone.com");
        		m.sendMail(new String[]{"szabolcs.toth2@gmail.com"}, null,null,"Előfizetői szerződés5 " + ((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date())), mailContent, null, "text/html; charset=utf-8", "szabolcs.toth@vodafone.com");
 */
        	}
            //if (m.init()) m.sendMail(new String[]{"szabolcs.toth2@gmail.com"}, null,null,"Előfizetői szerződés", mailContent, new File("conf/email.html"), "text/html; charset=utf-8", "szabolcs.toth@vodafone.com");
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
	private boolean init() {
        boolean ret = true;
        conf = new ConfigReader("conf/conf.ini",Log);
        ret = conf.getConfig();
        if (ret) {
            try {
            	numThreads = Integer.parseInt(conf.getConfString("SYSTEM", "THREADS"));
            	charSet = conf.getConfString("SYSTEM", "CharSet");
            	
                outputDir = conf.getConfString("SYSTEM", "OutputDir");  
                File f = new File(outputDir);
                
                if (! (f.exists() && f.isDirectory())) {
                	//INPUT Folder missing
                	Trace.write("Output directory does not exists! ("+outputDir+")");
                	ret = false;
                }

            	ret = readContractNum();

				outFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDir + "/outputFile_" + sdf.format(new Date()) + ".csv"),"UTF-8"));

				errFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDir + "/errorFile_" + sdf.format(new Date()) + ".csv"),"UTF-8"));

				linkFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDir + "/linkFile_" + sdf.format(new Date()) + ".csv"),"UTF-8"));

				inputDir = conf.getConfString("SYSTEM", "InputDir");  
                f = new File(inputDir);

                String[] templates = conf.getConfString("TEMPLATES", "WS_TEMPLATES").split(",");
                for (String type : templates) {
                	String templateFile = conf.getConfString("TEMPLATES", type);
                    Scanner s = new Scanner(new FileInputStream(new File(templateFile)),"UTF-8");
                    StringBuffer template = new StringBuffer(s.useDelimiter("\\Z").next());
                    while (template.length() > 0 && template.charAt(0) != '<') {
                    	template.deleteCharAt(0);
                    }
                    esb_templates.put(type, template);
                    s.close();
				}
                
                if (! (f.exists() && f.isDirectory())) {
                	//INPUT Folder missing
                	Trace.write("Input directory does not exists! ("+inputDir+")");
                	ret = false;
                } else {
	                files = f.listFiles();
	                fCounter = files.length;
	                Arrays.sort(files,Collections.reverseOrder());
                }
                
                for (int i = 0; i<numThreads;i++) {
                	ThreadedDocCreate t = new ThreadedDocCreate(Log,Trace,conf, this,Integer.toString(i));
                	Thread th = new Thread(t);
                	threadList.add(th);
                	th.start();
                }
                
                contract_prefix = conf.getConfString("SYSTEM", "CONTRACT_PREFIX");
                contractNumLen = Integer.parseInt(conf.getConfString("SYSTEM", "CONTRACT_NUM_LEN"));
                
            } catch (Exception se) {
                Log.write("Error during init! - " + se.getMessage());
                Trace.writeEXCEPTION(se);
                ret = false;
            }
        } else {
            Log.write("Error Reading Config file!");
        }
		return ret;
	}
	
	protected synchronized String getNextLine() {
		String line = null;
		try {
			if (inFile == null) {
				//inFile = new BufferedReader(new FileReader(files[(fCounter--)-1]));
				inFile = new BufferedReader(new InputStreamReader(new FileInputStream(files[(fCounter--)-1]), charSet));
				Trace.write("Opening file: " + files[(fCounter)].getName());
			}
			if ((line = inFile.readLine()) == null) {
				if (fCounter>0) {
					//inFile = new BufferedReader(new FileReader(files[(fCounter--)-1]));
					try {
						inFile.close();
					} catch (Exception e2) {
						//Do nothing
					}
					inFile = new BufferedReader(new InputStreamReader(new FileInputStream(files[(fCounter--)-1]), charSet));
					Trace.write("Opening file: " + files[(fCounter)].getName());
					line = inFile.readLine();
				}
			} 
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if (line != null) procCounter++;
		return line;
	}


	protected synchronized void storeERROR(String line) {
		try {
			//Trace.write(line);
			errFile.write(line + "\n");
			errFile.flush();
			errCounter++;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	protected synchronized void storeOutput(String line) {
		try {
			//Trace.write(line);
			outFile.write(line + "\n");
			outFile.flush();
			succCounter++;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected synchronized void storeLink(String line) {
		try {
			//Trace.write(line);
			linkFile.write(line + "\n");
			linkFile.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/**
     * Checks if any of the FileProcessor Threads are still alive.
     *
     * @return boolean - true if any of the threads is still running, false if all are destroyed.
     */
    private boolean threadsAlive() {
        boolean ret = false;
        Iterator<Thread> iter = threadList.iterator();
        while (iter.hasNext()) {
            Thread th = (Thread) iter.next();
            if (th.isAlive()) {
                ret = true;
                break;
            }
        }
        return ret;
    }	

	protected synchronized String getContractNum() {
		contractNum++;
		return contract_prefix + String.format("%0" + contractNumLen + "d", contractNum); 
	}
    

	protected synchronized int runCmd(String cmd) throws Exception{
		Process proc = Runtime.getRuntime().exec(cmd);
		int ret = proc.waitFor();

		if (true) {
			System.out.println("Command: " + cmd);
			BufferedInputStream buffer = new BufferedInputStream( proc.getInputStream() );
			BufferedReader commandOutput= new BufferedReader( new InputStreamReader( buffer ) );
			String line = null;
			try {
			      while ( ( line = commandOutput.readLine() ) != null ) {
			                        System.out.println( "command output: " + line );
			      }//end while
			      commandOutput.close(); 
			} catch ( IOException e) {
			}		
		}
		return ret;

	}

	protected synchronized int runCmd2(String cmd) throws Exception{
		Process proc = Runtime.getRuntime().exec(cmd);
		int ret = proc.waitFor();

		if (true) {
			System.out.println("Command: " + cmd);
			BufferedInputStream buffer = new BufferedInputStream( proc.getInputStream() );
			BufferedReader commandOutput= new BufferedReader( new InputStreamReader( buffer ) );
			String line = null;
			try {
			      while ( ( line = commandOutput.readLine() ) != null ) {
			                        System.out.println( "command output: " + line );
			      }//end while
			      commandOutput.close(); 
			} catch ( IOException e) {
			}		
		}
		return ret;

	}

	
}
