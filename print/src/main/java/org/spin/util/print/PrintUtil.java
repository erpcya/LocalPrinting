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

import java.awt.print.PrinterJob;
import java.util.logging.Logger;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

/**
 * Util class for get printers
 * @author Yamel Senih
 *
 */
public class PrintUtil {

	/**
	 *  Return PrinterJob with selected printer name.
	 *  @param printerName
	 *  @param defaultWhenNotFound
	 *  @param log
	 */
	public static PrinterJob getPrinterJob (String printerName, boolean defaultWhenNotFound, Logger log) {
		PrinterJob printerJob = null;
		PrintService printerService = null;
		try {
			printerJob = PrinterJob.getPrinterJob();
			//  find printer service
			if (printerName != null 
					&& printerName.length() != 0) {
				for (PrintService service : PrintServiceLookup.lookupPrintServices(null,null)) {
					String serviceName = service.getName();
					log.info("Service Name Available: " + serviceName);
					if (printerName.equals(serviceName)) {
						printerService = service;
						break;
					}
				}
			}   //  find printer service
			if(!defaultWhenNotFound
					&& printerService == null) {
				return null;
			}
			try {
				if (printerService != null) {
					printerJob.setPrintService(printerService);
				}
			} catch (Exception e) {
				log.warning("Could not set Print Service: " + e.toString());
			}
			//
			PrintService printerServiceUsed = printerJob.getPrintService();
			if (printerServiceUsed == null) {
				log.warning("Print Service not Found");
			} else {
				String serviceName = printerServiceUsed.getName();
				if (printerName != null 
						&& !printerName.equals(serviceName)) {
					log.warning("Not found: " + printerName + " - Used: " + serviceName);
				}
			}
		} catch (Exception e) {
			log.warning("Could not create for " + printerName + ": " + e.toString());
		}
		return printerJob;
	}   //  getPrinterJob
}
