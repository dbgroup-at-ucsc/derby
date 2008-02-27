/*

   Derby - Class org.apache.derby.impl.drda.NetworkServerMBeanImpl

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package org.apache.derby.impl.drda;

import org.apache.derby.mbeans.drda.NetworkServerMBean;
import org.apache.derby.iapi.reference.Property;
import org.apache.derby.iapi.services.monitor.Monitor;
import org.apache.derby.drda.NetworkServerControl;
import org.apache.derby.iapi.services.property.PropertyUtil;

/**
 * <p>
 * This is an implementation of the 
 * <code>org.apache.derby.mbeans.drda.NetworkServerMBean</code>,
 * providing management and monitoring capabilities related to the Network 
 * Server through JMX.</p>
 * <p>
 * This bean uses callbacks to the NetworkServerControlImpl class instead of
 * invoking NetworkServerControl, as it is the impl class that contains most
 * of the information we want to expose via JMX.</p>
 * 
 * @see org.apache.derby.mbeans.drda.NetworkServerMBean
 */
class NetworkServerMBeanImpl implements NetworkServerMBean {
    
    /* The instrumented server implementation */
    private NetworkServerControlImpl server;
    
    NetworkServerMBeanImpl(NetworkServerControlImpl nsc) {
        this.server = nsc;
    }

    // Some of the code is disabled (commented out) due to security concerns,
    // see DERBY-1387 for details.
    
    //
    // ------------------------- MBEAN ATTRIBUTES  ----------------------------
    //
    
    public String getDrdaHost() {
        String host = getServerProperty(Property.DRDA_PROP_HOSTNAME);
        return host;
    }
    
    public boolean getDrdaKeepAlive() {
        String on = getServerProperty(Property.DRDA_PROP_KEEPALIVE);
        return ( "true".equals(on) ? true : false);
    }
    
    public int getDrdaMaxThreads() {
        int maxThreads = 0; // default
        String maxThreadsStr = getServerProperty(Property.DRDA_PROP_MAXTHREADS);
        if (maxThreadsStr != null) {
            try {
                maxThreads = Integer.parseInt(maxThreadsStr);
            } catch (NumberFormatException nfe) {
                // ignore, use the default value
            }
        }
        return maxThreads;
    }
    
    /*
    public void setDrdaMaxThreads(int max)
        throws Exception
    {
        try {
            server.netSetMaxThreads(max);
        } catch (Exception ex) {
            Monitor.logThrowable(ex);
            throw ex;
        }
    }*/
    
    public int getDrdaPortNumber() {
        int portNumber = NetworkServerControl.DEFAULT_PORTNUMBER; // the default
        String portString = getServerProperty(Property.DRDA_PROP_PORTNUMBER);
        try {
            portNumber = Integer.parseInt(portString);
        } catch (NumberFormatException nfe) {
            // ignore, use the default value
        }
        return portNumber;
    }
    
    public String getDrdaSecurityMechanism() {
        String secmec = getServerProperty(Property.DRDA_PROP_SECURITYMECHANISM);
        if (secmec == null) {
            // default is none (represented by the empty string)
            secmec = "";
        }
        return secmec;
    }
    
    public String getDrdaSslMode() {
        // may be null if not set (?)
        String SSLMode = getServerProperty(Property.DRDA_PROP_SSL_MODE);
        return SSLMode;
    }
    
    
    public String getDrdaStreamOutBufferSize() {
        // TODO - Fix NetworkServerControlImpl so that this setting is included
        //        in the property values returned by getPropertyValues()?
        //String size = getServerProperty(Property.DRDA_PROP_STREAMOUTBUFFERSIZE);
        String size = PropertyUtil.getSystemProperty(
                Property.DRDA_PROP_STREAMOUTBUFFERSIZE, "0");
        return size;
    }

       
    public int getDrdaTimeSlice() {
        // relying on server to return the default if not set
        return server.getTimeSlice();
    }
    
    /*
    public void setDrdaTimeSlice(int timeSlice)
        throws Exception
    {
        try {
            server.netSetTimeSlice(timeSlice);
        } catch (Exception ex) {
            Monitor.logThrowable(ex);
            throw ex;
        }
    }*/
    
    public boolean getDrdaTraceAll() {
        String on = getServerProperty(Property.DRDA_PROP_TRACEALL);
        return ("true".equals(on) ? true : false );
    }
    
    /*
    public void setDrdaTraceAll(boolean on)
        throws Exception
    {
        try {
            server.trace(on);
        } catch (Exception ex) {
            Monitor.logThrowable(ex);
            throw ex;
        }
    }*/
    
    public String getDrdaTraceDirectory() {
        String traceDirectory = null;
        traceDirectory = getServerProperty(Property.DRDA_PROP_TRACEDIRECTORY);
        if(traceDirectory == null){
            // if traceDirectory is not set, derby.system.home is default
            traceDirectory = getServerProperty(Property.SYSTEM_HOME_PROPERTY);
        }
        
        // if derby.system.home is not set, current directory is default
        if (traceDirectory == null) {
            traceDirectory = PropertyUtil.getSystemProperty("user.dir");
        }
        return traceDirectory;
    }
    
    /*
    public void setDrdaTraceDirectory(String dir)
        throws Exception
    {
        try {
            server.sendSetTraceDirectory(dir);
        } catch (Exception ex) {
            Monitor.logThrowable(ex);
            throw ex;
        }
    }*/
    
    /*
    public String getSysInfo()
        throws Exception
    {
        String sysInfo = "";
        try {
            sysInfo = server.sysinfo();
            // remove information that is also given in the DerbySystemMBean
            return sysInfo.substring(sysInfo.indexOf("DRDA"),sysInfo.indexOf("-- list"));
        } catch (Exception ex) {
            Monitor.logThrowable(ex);
            throw ex;
        }
    }
     **/
    
    
    
     
    // ------------------------- MBEAN OPERATIONS  ----------------------------
    
    /**
     * Pings the Network Server.
     * 
     * @see org.apache.derby.mbeans.drda.NetworkServerMBean#ping()
     * @throws Exception if the ping fails.
     */
    public void ping() throws Exception {
        //String feedback = "Server pinged successfully.";
        //boolean success = true;
        try {
            server.ping();
        } catch (Exception ex) {
            Monitor.logThrowable(ex);
            //feedback = "Error occured while pinging server.";
            //success = false;
            throw ex;
        }
    }
    
    /*
    public String traceConnection(int connection, boolean on)
        throws Exception
    {
        String feedback;
        if(on){
            feedback = "Tracing enabled for connection " + connection
                + ". \n (0 = all connections)";
        }
        else{
            feedback = "Tracing disabled for connection " + connection
                + ". \n (0 = all connections)";           
        }
        try {
            server.trace(connection, on);
        } catch (Exception ex) {
            Monitor.logThrowable(ex);
            throw ex;
        }
        return feedback;
    }
     */
    
    /*
    public String enableConnectionLogging()
        throws Exception
    {
        String feedback = "Connection logging enabled.";
        try {
            server.logConnections(true);
        } catch (Exception ex) {
            Monitor.logThrowable(ex);
            throw ex;
        }
        
        return feedback;
    }*/
   
    /*
    public String disableConnectionLogging()
        throws Exception
    {
        String feedback = "Connection logging disabled.";
        try {
            server.logConnections(false);
        } catch (Exception ex) {
            Monitor.logThrowable(ex);
            throw ex;
        }
       
       return feedback;
    }*/
    
    /*
    public void shutdown()
        throws Exception
    {
        try {
            server.shutdown();
        } catch (Exception ex) {
            Monitor.logThrowable(ex);
            throw ex;
        }
    }*/
   
   // ------------------------- UTILITY METHODS  ----------------------------
    
   /**
    *  Gets the value of a specific network server setting (property). 
    *  Most server-related property keys have the prefix 
    *  <code>derby.drda.</code> and may be found in the 
    *  org.apache.derby.iapi.reference.Property class.
    * 
    *  @see org.apache.derby.iapi.reference.Property
    *  @param property the name of the server property
    *  @return the value of the given server property
    */
   private String getServerProperty(String property) {
        return server.getPropertyValues().getProperty(property);     
   }

}