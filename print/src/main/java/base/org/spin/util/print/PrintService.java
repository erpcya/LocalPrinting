/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it    		 *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope   		 *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 		 *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           		 *
 * See the GNU General Public License for more details.                       		 *
 * You should have received a copy of the GNU General Public License along    		 *
 * with this program; if not, write to the Free Software Foundation, Inc.,    		 *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     		 *
 * For the text or an alternative of this public license, you may reach us    		 *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpya.com				  		                 *
 *************************************************************************************/

package org.spin.util.print;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;

import org.apache.activemq.ActiveMQConnectionFactory;
 
/**
 * Print Service from Queue
 * @author Yamel Senih
 *
 */
public class PrintService implements Runnable {
	
    public static void main(String[] args) throws Exception {
    	//	Validate
    	if(args == null) {
    		throw new Exception("Arguments Not Found");
    	}
    	//	
    	if(args.length < 5) {
    		throw new Exception("Arguments Must Be: [Host, User, Password, Queue, Printer Home, Printer Name, Connetion Interval]");
    	}
    	//	Get sleep interval
    	long connectionInterval = 0;
    	if(args.length > 6) {
    		connectionInterval = Long.parseLong(args[6]);
    	}
    	//	Get Parameters
    	PrintService service = new PrintService(args[0], args[1], args[2], args[3], args[4], args[5], connectionInterval);
    	service.start();
    }
    
    /**
     * 
     * @param host
     * @param user
     * @param password
     * @param queue
     * @param printerName
     * @param homeFolder
     * @param connectionInterval
     */
    public PrintService(String host, String user, String password, String queue, String homeFolder, String printerName, long connectionInterval) {
    	this.host = host;
    	this.user = user;
    	this.password = password;
    	this.queue = queue;
    	this.printerName = printerName;
    	this.homeFolder = homeFolder;
    	if(connectionInterval > 0) {
    		this.connectionInterval = connectionInterval;
    	}
    	getValidateHomeFolder();
    	createMainFolder();
    	saveLog();
    }
    
    /**	flag	*/
    private final AtomicBoolean running = new AtomicBoolean(false);
    /**	Host	*/
    private String host;
    /**	User	*/
    private String user;
    /**	Password	*/
    private String password;
    /**	Queue	*/
    private String queue;
    /**	Printer Name	*/
    private String printerName;
    /**	String Home Directory	*/
    private String homeFolder;
    /**	Interval	*/
    private long connectionInterval = 5000;
    /**	Thread	*/
    private Thread worker;
    /**	Connection	*/
    private Connection connection;
    /**	Session	*/
    private Session session;
    /**	Consumer	*/
    private MessageConsumer consumer;
    /**	Constants	*/
    private final String LOG_FOLDER = "log";
    private final String BACKUP_FOLDER = "backup";
    private final String MAIN_FOLDER = "PrintService";
    /**	Format	*/
    private final String FORMAT = "yyyyMMdd_hhmmss";
    /**	Log	*/
    private Logger log = Logger.getLogger(PrintService.class.getName());
    
    /**
     * Create connection
     * @return
     */
    private boolean createConnection() {
    	if(connection != null) {
    		return true;
    	}
    	try {
    		// Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, host);
            // Create a Connection
            connection = connectionFactory.createConnection();
            connection.start();
            // Create a Session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // Create the destination (Topic or Queue)
            Destination destination = session.createQueue(queue);
            // Create a MessageConsumer from the Session to the Topic or Queue
            consumer = session.createConsumer(destination);
            //	
            log.info("Connection created");
    	} catch (JMSException e) {
    		log.severe(e.getLocalizedMessage());
			return false;
		}
        return true;
    }
    
    /**
     * Validate Home folder
     */
    private void getValidateHomeFolder() {
    	if(homeFolder.trim().endsWith(File.pathSeparator)) {
    		homeFolder = homeFolder.substring(0, homeFolder.length() -1);
    	}
    }
    
    /**
     * Get home folder
     * @return
     */
    private String getHomeFolder() {
    	return homeFolder;
    }
    
    /**
     * Get folder used for write log
     * @return
     */
    private String getLogFolder() {
    	return getMainFolder() + File.separator + LOG_FOLDER;
    }
    
    /**
     * get folder used for write files to print
     * @return
     */
    private String getBackupFolder() {
    	return getMainFolder() + File.separator + BACKUP_FOLDER;
    }
    
    /**
     * Get main folder
     * @return
     */
    private String getMainFolder() {
    	return getHomeFolder() + File.separator + MAIN_FOLDER;
    }
    
    /**
     * Print Document
     * @param byteDocument
     * @param jobName
     * @throws PrinterException
     * @throws IOException
     * @throws PrintException
     */
    private void printDocument(byte[] byteDocument, String jobName) {
    	if(printerName == null) {
    		log.severe("Printer Name Not Found");
    		return;
    	}
    	try {
	    	//	Get Printer Job
	    	PrinterJob printerJob = PrintUtil.getPrinterJob(printerName, false, log);
	    	//	Validate
	    	if(printerJob == null) {
	    		log.severe("Printer not found [" + printerName + "]");
	    		return;
	    	}
	    	//	Set Job Name
	    	if(jobName != null) {
	    		printerJob.setJobName(jobName);
	    	}
	    	//	Create Print document
	    	Doc document = new SimpleDoc(byteDocument, DocFlavor.BYTE_ARRAY.PDF, null);
	    	//	Create Print Job
	    	DocPrintJob printJob = printerJob.getPrintService().createPrintJob();
	    	//	Print Document
			printJob.print(document, new HashPrintRequestAttributeSet());
		} catch (PrintException e) {
			log.severe(e.getLocalizedMessage());
		}
    }
    
    /**
     * Get Name for files
     * @return
     */
    private String getDateName() {
    	return new SimpleDateFormat(FORMAT).format(new Date(System.currentTimeMillis()));
    }
    
    /**
     * Create Main Folder
     */
    private void createMainFolder() {
    	try {
    		File file = new File(getMainFolder());
			if(!file.exists()) {
				file.mkdirs();
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
		}   
    }
    
    /**
     * Save Log
     */
    private void saveLog() {
    	try {
    		File file = new File(getLogFolder());
			if(!file.exists()) {
				file.mkdirs();
			}
			FileHandler fileHandler = new FileHandler(getLogFolder() + File.separator + getDateName() + "_PrintService.log");
			log.addHandler(fileHandler);  
	        fileHandler.setFormatter(new SimpleFormatter());
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
		}   
    }
    
    /**
     * Close Connection
     * @return
     */
    private boolean closeConnection() {
    	if(connection == null) {
    		return false;
    	}
    	try {
    		if(consumer == null
    				|| session == null
    				|| connection == null) {
    			log.severe("Connection is lost");
    			return false;
    		}
			consumer.close();
			session.close();
	        connection.close();
            //	
            log.info("Connection closed");
		} catch (JMSException e) {
			e.printStackTrace();
			return false;
		} finally {
			consumer = null;
	        session = null;
	        connection = null;
		}
        return true;
    }
    
    /**
     * Backup file
     * @param document
     */
    private void backupFile(byte[] document, String fileName) {
    	if(fileName == null) {
    		log.severe("File name Not Found");
    		return;
    	}
    	//	Save
    	fileName = getDateName() + "_" + fileName;
		try {
			File file = new File(getBackupFolder());
			if(!file.exists()) {
				file.mkdirs();
			}
			FileOutputStream fileOutputStream = new FileOutputStream(getBackupFolder() + File.separator + fileName);
			fileOutputStream.write(document);
			fileOutputStream.close();
			log.info("File write: " + fileName);
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
		}
    }
    
    /**
     * Receive Message
     * @throws JMSException
     * @throws IOException
     */
    private void receive() throws JMSException, IOException {
    	if(!createConnection()) {
    		return;
    	}
    	// Wait for a message
        Message message = consumer.receive();
        if(message instanceof BytesMessage) {
        	BytesMessage bytesMessage = (BytesMessage) message;
        	byte[] buffer = new byte[(int) bytesMessage.getBodyLength()];
        	bytesMessage.readBytes(buffer);
        	String jobName = bytesMessage.getStringProperty("JobName");
        	String fileName = bytesMessage.getStringProperty("FileName");
        	//	Save to backup
        	backupFile(buffer, fileName);
        	//	Print it
        	printDocument(buffer, jobName);
    		log.info("Messge readed");
        } else {
        	log.info("Received: " + message);
        }
    }
    
    /**
     * Start thread
     */
    public void start() {
    	log.info("Thread created");
        worker = new Thread(this);
        worker.start();
    }
  
    /**
     * Stop Thread
     */
    public void stop() {
    	running.set(false);
    }
    
	@Override
	public void run() {
		running.set(true);
        while (running.get()) {
        	try {
        		log.info("Reading...");
        		receive();
        		log.info("Sleeping...");
        		Thread.sleep(connectionInterval);
            } catch (InterruptedException e){ 
                Thread.currentThread().interrupt();
                log.severe("Thread was interrupted, Failed to complete operation");
            } catch (Exception e) {
				closeConnection();
				log.severe(e.getLocalizedMessage());
			}
        }
        //	Close Connection
        closeConnection();
	}
}
