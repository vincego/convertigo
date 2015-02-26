package com.twinsoft.convertigo.beans.connectors;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractDatabaseTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.CouchDbParameter;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;

public class FullSyncConnector extends CouchDbConnector {
	private static final long serialVersionUID = 4063707392313093177L;
	
	@Override
	public CouchClient getCouchClient() {
		return Engine.theApp.couchDbManager.getCouchClient();
	}
	
	@Override
	public String getDatabaseName() {
		return getName();
	};
	
	@Override
	public List<CouchDbParameter> filter(CouchDbParameter... parameters) {
		int index = ArrayUtils.indexOf(parameters, CouchDbParameter.Path_database);
		if (index != -1) {
			parameters = ArrayUtils.remove(parameters, index);
		}
		return super.filter(parameters);
	}
	
	@Override
	public String getTargetDatabase(AbstractDatabaseTransaction couchDbTransaction) {
		return getDatabaseName();
	}
}
