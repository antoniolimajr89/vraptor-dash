package br.com.caelum.vraptor.dash.uristats;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.com.caelum.vraptor.InterceptionException;
import br.com.caelum.vraptor.core.InterceptorStack;
import br.com.caelum.vraptor.dash.cache.ObservableResponse;
import br.com.caelum.vraptor.http.VRaptorResponse;
import br.com.caelum.vraptor.interceptor.Interceptor;
import br.com.caelum.vraptor.ioc.Container;
import br.com.caelum.vraptor.resource.ResourceMethod;

/**
 * Saves all request information as statistics
 * 
 * @author guilherme silveira
 */
public class BaseURIStatInterceptor implements Interceptor {
	
	private final HttpServletRequest request;
	private final Container container;
	private final HttpServletResponse response;

	public BaseURIStatInterceptor(Container container, 
			HttpServletRequest request, HttpServletResponse response) {
		this.container = container;
		this.request = request;
		this.response = response;
	}

	public boolean accepts(ResourceMethod arg0) {
		return true;
	}

	public void intercept(InterceptorStack stack, ResourceMethod method,
			Object instance) throws InterceptionException {
		long before = System.currentTimeMillis();
		stack.next(method, instance);
		long time = System.currentTimeMillis() - before;

		String key = userKey(container);

		String resource = method.getResource().getType().getName();
		String methodName = method.getMethod().getName();

		String etag = "";
		String cacheControl = "";
		String hadEtag = "unknown";
		long size = 0;
		int status = 200;
		if (response instanceof VRaptorResponse) {
			VRaptorResponse r = (VRaptorResponse) response;
			ServletResponse sr = r.getResponse();
			if(sr instanceof ObservableResponse) {
				ObservableResponse resp = (ObservableResponse) sr;
				etag = resp.getEtagHeader();
				if(etag.equals("")) {
					etag = resp.getMd5();
					hadEtag = "false";
				} else {
					hadEtag = "true";
				}
				size = resp.size();
				cacheControl = resp.getCacheControlHeader();
				status = resp.getGivenStatus();
			}
		}
		
		Stat stat = new Stat(key, request.getRequestURI(), time,
				request.getMethod(), resource, methodName,
				etag, status, hadEtag,
				cacheControl, size);
		
		try {
			saveStat(stat);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Saves this stat where you want
	 * @param stat
	 */
	protected void saveStat(Stat stat) {
		stat.log();
	}
	
	public static String userKey(Container container) {
		String key = "";
		try {
			IdeableUser user = container.instanceFor(IdeableUser.class);
			key = user.getId().toString();
		} catch (Exception e) {
			key = "";
		}
		return key;
	}

}