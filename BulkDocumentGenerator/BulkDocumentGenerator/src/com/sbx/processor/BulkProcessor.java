/**
 * 
 */
package com.sbx.processor;

import java.util.HashMap;

import com.sbx.webService.WebServiceParams;

/**
 * @author szabolcs.toth
 *
 */
public interface BulkProcessor {

	public HashMap<String,Object> processBulkRequest(WebServiceParams ws_params) throws Exception;
}
