/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package lucee.intergral.fusiondebug.server;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lucee.commons.io.SystemUtil;
import lucee.commons.lang.SystemOut;
import lucee.runtime.CFMLFactory;
import lucee.runtime.CFMLFactoryImpl;
import lucee.runtime.Info;
import lucee.runtime.PageContextImpl;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigWebImpl;
import lucee.runtime.engine.CFMLEngineImpl;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.op.Caster;
import lucee.runtime.security.SerialNumber;

import com.intergral.fusiondebug.server.IFDController;
import com.intergral.fusiondebug.server.IFDThread;

/**
 * 
 */
public class FDControllerImpl implements IFDController {


	private List exceptionTypes;
	private CFMLEngineImpl engine;
	private boolean isEnterprise;
	
	
	public FDControllerImpl(CFMLEngineImpl engine,String serial){
		this.isEnterprise=SerialNumber.isEnterprise(serial);
		this.engine=engine;
	}

	@Override
	public String getEngineName() {
		return "Lucee";
	}

	@Override
	public String getEngineVersion() {
		return Info.getVersionAsString();
	}

	@Override
	public List getExceptionTypes() {
		if(exceptionTypes==null){
			exceptionTypes=new ArrayList();
			exceptionTypes.add("application");
			exceptionTypes.add("expression");
			exceptionTypes.add("database");
			exceptionTypes.add("custom_type");
			exceptionTypes.add("lock");
			exceptionTypes.add("missinginclude");
			exceptionTypes.add("native");
			exceptionTypes.add("security");
			exceptionTypes.add("template");
		}
		return exceptionTypes;
	}

	/**
	 * @deprecated use instead <code>{@link #getLicenseInformation(String)}</code>
	 */
	public String getLicenseInformation() {
		throw new RuntimeException("please replace your fusiondebug-api-server-1.0.xxx-SNAPSHOT.jar with a newer version");
	}

	@Override
	public String getLicenseInformation(String key) {
		if(!isEnterprise) {
			SystemOut.print(new PrintWriter(System.err),"FD Server Licensing does not work with the Open Source Version of Lucee or Enterprise Version of Lucee that is not enabled");
			return null;
		}
		return FDLicense.getLicenseInformation(key);
	}


	@Override
	public void output(String message) {
		Config config = ThreadLocalPageContext.getConfig();
		PrintWriter out=config==null?SystemUtil.getPrintWriter(SystemUtil.OUT):((ConfigWebImpl)config).getOutWriter();
		SystemOut.print(out, message);
	}

	@Override
	public List pause() {
		List<IFDThread> threads = new ArrayList<IFDThread>();
		Iterator<Entry<String, CFMLFactory>> it = engine.getCFMLFactories().entrySet().iterator();
		Entry<String, CFMLFactory> entry;
		while(it.hasNext()){
			entry = it.next();
			pause(entry.getKey(),(CFMLFactoryImpl) entry.getValue(), threads);
		}
		
		return threads;
	}
	
	private void pause(String name,CFMLFactoryImpl factory,List<IFDThread> threads) {
		Map<Integer, PageContextImpl> pcs = factory.getActivePageContexts();
		Iterator<PageContextImpl> it = pcs.values().iterator();
		PageContextImpl pc;
		
		while(it.hasNext()){
			pc=it.next();
			try {
				pc.getThread().wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			threads.add(new FDThreadImpl(this,factory,name,pc));
		}
	}
	
	@Override
	public boolean getCaughtStatus(
			String exceptionType,
			String executionUnitName,
            String executionUnitPackage,
            String sourceFilePath,
            String sourceFileName,
            int lineNumber) {
		// TODO [007]
		return true;
	}

	@Override
	public IFDThread getByNativeIdentifier(String id) {
		Iterator<Entry<String, CFMLFactory>> it = engine.getCFMLFactories().entrySet().iterator();
		Entry<String, CFMLFactory> entry;
		FDThreadImpl thread;
		while(it.hasNext()){
			entry = it.next();
			thread = getByNativeIdentifier( entry.getKey(),(CFMLFactoryImpl) entry.getValue(),id);
			if(thread!=null) return thread;
		}
		return null;
	}
	
	/**
	 * checks a single CFMLFactory for the thread
	 * @param name
	 * @param factory
	 * @param id
	 * @return matching thread or null
	 */
	private FDThreadImpl getByNativeIdentifier(String name,CFMLFactoryImpl factory,String id) {
		Map<Integer, PageContextImpl> pcs = factory.getActivePageContexts();
		Iterator<PageContextImpl> it = pcs.values().iterator();
		PageContextImpl pc;
		
		while(it.hasNext()){
			pc=it.next();
			if(equals(pc,id)) return new FDThreadImpl(this,factory,name,pc);
		}
		return null;
	}

	/**
	 * check if thread of PageContext match given id
	 * @param pc
	 * @param id
	 * @return match the id the pagecontext
	 */
	private boolean equals(PageContextImpl pc, String id) {
		Thread thread = pc.getThread();
		if(Caster.toString(FDThreadImpl.id(pc)).equals(id)) return true;
		if(Caster.toString(thread.getId()).equals(id)) return true;
		if(Caster.toString(thread.hashCode()).equals(id)) return true;
		return false;
	}

	@Override
	public String getCompletionMethod() {
		return "serviceCFML";
	}

	@Override
	public String getCompletionType() {
		return CFMLEngineImpl.class.getName();
	}

	@Override
	public void release() {
		this.engine.allowRequestTimeout(true);
	}
}
