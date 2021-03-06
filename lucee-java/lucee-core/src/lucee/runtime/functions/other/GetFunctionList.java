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
/**
 * Implements the CFML Function getfunctionlist
 */
package lucee.runtime.functions.other;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import lucee.runtime.PageContext;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.transformer.library.function.FunctionLib;
import lucee.transformer.library.function.FunctionLibFunction;
import lucee.transformer.library.tag.TagLib;

public final class GetFunctionList implements Function {
	
	private static Struct functions;
	
	public synchronized static lucee.runtime.type.Struct call(PageContext pc) throws PageException {
		
		
		if(functions==null) {
			Struct sct=new StructImpl();
			//synchronized(sct) {
				//hasSet=true;
			FunctionLib[] flds;
			flds = ((ConfigImpl)pc.getConfig()).getFLDs();
			FunctionLibFunction func;
			Map<String, FunctionLibFunction> _functions;
			Iterator<Entry<String, FunctionLibFunction>> it;
			Entry<String, FunctionLibFunction> e;
			for(int i=0;i<flds.length;i++) {
				_functions = flds[i].getFunctions();
				it = _functions.entrySet().iterator();
				
				while(it.hasNext()){
					e = it.next();
					func = e.getValue();
					if(func.getStatus()!=TagLib.STATUS_HIDDEN && func.getStatus()!=TagLib.STATUS_UNIMPLEMENTED)
						sct.set(e.getKey(),"");
				}
			}
			functions=sct;
			//}
		}
		return functions;
	}
}