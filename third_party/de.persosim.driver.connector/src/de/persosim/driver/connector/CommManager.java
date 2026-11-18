package de.persosim.driver.connector;

import java.util.LinkedList;
import java.util.List;

import org.globaltester.logging.BasicLogger;

import de.persosim.driver.connector.exceptions.IfdCreationException;

public class CommManager {
	
	private static List<CommProvider> providers = new LinkedList<>();
	
	static {
		providers.add(new CommProvider() {
			
			@Override
			public IfdComm get(String type) {
				if ("VIRTUAL".equals(type) || type == null){
					try {
						return new VirtualDriverComm(VirtualDriverComm.DEFAULT_HOST, VirtualDriverComm.DEFAULT_PORT);
					} catch (IfdCreationException e) {
						BasicLogger.logException(getClass(), "Could not create virtual driver comm", e);
					}
				}
				return null;
			}
		});
	}
	
	public static void addCommProvider(CommProvider provider){
		providers.add(provider);
	}
	
	public static IfdComm getCommForType(String type){
		for (CommProvider provider : providers){
			IfdComm candidate = provider.get(type);
			if (candidate != null){
				return candidate;
			}
		}
		return null;
	}

	public static void removeCommProvider(CommProvider provider) {
		providers.remove(provider);
	}

	public static IfdComm getWorkingComm(String preferred) {
		IfdComm candidate = getCommForType(preferred);
		if (candidate == null) {
			for (CommProvider provider : providers) {
				candidate = provider.get(null);
				if (candidate != null) {
					return candidate;
				}
			}
		}
		return null;
	}
}
