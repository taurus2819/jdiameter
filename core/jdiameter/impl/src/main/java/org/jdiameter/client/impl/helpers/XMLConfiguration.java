/*
 * Copyright (c) 2006 jDiameter.
 * https://jdiameter.dev.java.net/
 *
 * License: GPL v3
 *
 * e-mail: erick.svenson@yahoo.com
 *
 */
package org.jdiameter.client.impl.helpers;

import org.jdiameter.api.Configuration;

import static org.jdiameter.client.impl.helpers.Parameters.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * This class provide loading and verification configuration from XML file
 */
public class XMLConfiguration extends EmptyConfiguration {

    /**
     * Create instance of class and load file from defined input stream
     * 
     * @param in input stream
     * @throws Exception
     */
    public XMLConfiguration(InputStream in) throws Exception {
        this(in, null, null, false);
    }

    /**
     * Create instance of class and load file from defined input stream
     * 
     * @param in input stream
     * @param attributes attributes for DocumentBuilderFactory
     * @param  features features for DocumentBuilderFactory
     * @throws Exception
     */
    public XMLConfiguration(InputStream in, Hashtable<String, Object> attributes, Hashtable<String, Boolean> features) throws Exception {
        this(in, attributes, features, false);
    }

    /**
     * Create instance of class and load file from defined  file name
     * 
     * @param filename configuration file name
     * @throws Exception
     */
    public XMLConfiguration(String filename) throws Exception {
        this(filename, null, null, false);
    }

    /**
     * Create instance of class and load file from defined input stream
     * 
     * @param filename configuration file name
     * @param attributes attributes for DocumentBuilderFactory
     * @param  features features for DocumentBuilderFactory
     * @throws Exception
     */
    public XMLConfiguration(String filename, Hashtable<String, Object> attributes, Hashtable<String, Boolean> features) throws Exception {
        this(filename, attributes, features, false);
    }

    protected XMLConfiguration(Object in, Hashtable<String, Object> attributes, Hashtable<String, Boolean> features, boolean nop) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        if (attributes != null)
            for (String key : attributes.keySet())
                factory.setAttribute(key, attributes.get(key));
        if (features != null)
            for (String key : features.keySet())
                factory.setFeature(key, features.get(key));
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document;

        if (in instanceof InputStream) {
            document = builder.parse((InputStream) in);
        }
        else if (in instanceof String) {
            document = builder.parse(new File((String) in));
        }
        else {
            throw  new Exception("Unknown type of input data");
        }
        validate(document);
        processing(document);
    }

    protected void validate(Document document) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(getClass().getResourceAsStream("/META-INF/jdiameter-client.xsd"));
        Schema schema = factory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        validator.validate(new DOMSource(document));
    }

    protected void processing(Document document) {
        Element element = document.getDocumentElement();
        NodeList c = element.getChildNodes();
        for (int i = 0; i < c.getLength(); i++) {
            String nodeName = c.item(i).getNodeName();
            if (nodeName.equals("LocalPeer")) {
              addLocalPeer(c.item(i));
            }
            else if (nodeName.equals("Parameters")) {
              addParameters(c.item(i));
            }
            else if (nodeName.equals("Network")) {
              addNetwork(c.item(i));
            }
            else if (nodeName.equals("Security")) {
              addSecurity(c.item(i));
            }
            else if (nodeName.equals("Extensions")) {
              addExtensions(c.item(i));
            }
        }
    }

    protected void addLocalPeer(Node node) {
        NodeList c = node.getChildNodes();
        if (node.getAttributes().getNamedItem("security_ref") != null) {
            add(SecurityRef, node.getAttributes().getNamedItem("security_ref").getNodeValue());
        }
        
        for (int i = 0; i < c.getLength(); i++) {
            String nodeName = c.item(i).getNodeName();
            if (nodeName.equals("URI")) {
              add(OwnDiameterURI, getValue(c.item(i)));
            }
            addIPAddress(c.item(i));
            if (nodeName.equals("Realm")) {
              add(OwnRealm, getValue(c.item(i)));
            }
            if (nodeName.equals("VendorID")) {
              add(OwnVendorID, getLongValue(c.item(i)));
            }
            if (nodeName.equals("ProductName")) {
              add(OwnProductName, getValue(c.item(i)));
            }
            if (nodeName.equals("FirmwareRevision")) {
              add(OwnFirmwareRevision, getLongValue(c.item(i)));
            }
            if (nodeName.equals("Applications")) {
              addApplications(c.item(i));
            }
        }
    }

    protected void addIPAddress(Node node) {
        String nodeName = node.getNodeName();
        if (nodeName.equals("IPAddress"))
            add(OwnIPAddress, getValue(node));
    }

    protected void addApplications(Node node) {
        NodeList c = node.getChildNodes();
        ArrayList<Configuration> items = new ArrayList<Configuration>();
        for (int i = 0; i < c.getLength(); i++) {
            String nodeName = c.item(i).getNodeName();
            if (nodeName.equals("ApplicationID")) {
                items.add(addApplication(c.item(i)));
            }
        }
        add(ApplicationId, items.toArray(EMPTY_ARRAY));
    }

    protected Configuration addApplication(Node node) {
        NodeList c = node.getChildNodes();
        AppConfiguration e = getInstance();
        for (int i = 0; i < c.getLength(); i++) {
            String nodeName = c.item(i).getNodeName();
            if (nodeName.equals("VendorId")) {
              e.add(VendorId, getLongValue(c.item(i)));
            }
            else if (nodeName.equals("AuthApplId")) {
              e.add(AuthApplId, getLongValue(c.item(i)));
            }
            else if (nodeName.equals("AcctApplId")) {
              e.add(AcctApplId, getLongValue(c.item(i)));
            }
        }
        return e;
    }

    protected void addParameters(Node node) {
        NodeList c = node.getChildNodes();
        for (int i = 0; i < c.getLength(); i++) {
            String nodeName = c.item(i).getNodeName();
            if (nodeName.equals("UseUriAsFqdn")) { add(UseUriAsFqdn, Boolean.valueOf(getValue(c.item(i))));   }
            else if (nodeName.equals("QueueSize")) { add(QueueSize, getIntValue(c.item(i)));                  }
            else if (nodeName.equals("MessageTimeOut")) { add(MessageTimeOut, getLongValue(c.item(i)));       }
            else if (nodeName.equals("StopTimeOut")) { add(StopTimeOut, getLongValue(c.item(i)));             }
            else if (nodeName.equals("CeaTimeOut")) { add(CeaTimeOut, getLongValue(c.item(i)));               }
            else if (nodeName.equals("IacTimeOut")) { add(IacTimeOut, getLongValue(c.item(i)));               }
            else if (nodeName.equals("DwaTimeOut")) { add(DwaTimeOut, getLongValue(c.item(i)));               }
            else if (nodeName.equals("DpaTimeOut")) { add(DpaTimeOut, getLongValue(c.item(i)));               }
            else if (nodeName.equals("RecTimeOut")) { add(RecTimeOut, getLongValue(c.item(i)));               }
            else if (nodeName.equals("SessionDatasource")) { add(SessionDatasource, getValue(c.item(i)));     }
            else if (nodeName.equals("TimerFacility")) { add(TimerFacility, getValue(c.item(i)));             }
            else if (nodeName.equals("Statistics")) { addStatisticLogger(Statistics, c.item(i));    }
            else if (nodeName.equals("Concurrent")) { addConcurrent(Concurrent, c.item(i));                   }
            else if (nodeName.equals("Dictionary")) { addDictionary(Dictionary, c.item(i));                   }
            
            else 
            appendOtherParameter(c.item(i));
        }
    }  

    protected void addConcurrent(org.jdiameter.client.impl.helpers.Parameters name, Node node) {
        NodeList c = node.getChildNodes();
        List<Configuration> items = new ArrayList<Configuration>();
        for (int i = 0; i < c.getLength(); i++) {
            String nodeName = c.item(i).getNodeName();
            if (nodeName.equals("Entity")) addConcurrentEntity(items, c.item(i));
        }
        add(name, items.toArray(new Configuration[items.size()]));
    }

    protected void addConcurrentEntity(List<Configuration> items, Node node) {
        AppConfiguration cfg = getInstance();
        String name = node.getAttributes().getNamedItem("name").getNodeValue();
        cfg.add(ConcurrentEntityName, name);
        if (node.getAttributes().getNamedItem("description") != null) {
            String descr = node.getAttributes().getNamedItem("description").getNodeValue();
            cfg.add(ConcurrentEntityDescription, descr);
        }
        if (node.getAttributes().getNamedItem("size") != null) {
            String size = node.getAttributes().getNamedItem("size").getNodeValue();
            cfg.add(ConcurrentEntityPoolSize, Integer.parseInt(size));
        }
        items.add(cfg);
    }

	protected void addStatisticLogger(org.jdiameter.client.impl.helpers.Parameters name, Node node) {
		String pause = node.getAttributes().getNamedItem("pause").getNodeValue();
		String delay = node.getAttributes().getNamedItem("delay").getNodeValue();
		String enabled = node.getAttributes().getNamedItem("enabled").getNodeValue();
		String active_records;
		if (node.getAttributes().getNamedItem("active_records") != null) {
			active_records = node.getAttributes().getNamedItem("active_records").getNodeValue();
		} else {
			active_records = (String) StatisticsActiveList.defValue();
		}
		add(name,
				getInstance().
				add(StatisticsLoggerPause, Long.parseLong(pause)).
				add(StatisticsLoggerDelay, Long.parseLong(delay)).
				add(StatisticsEnabled, Boolean.parseBoolean(enabled)).
				add(StatisticsActiveList, active_records));
	}

    protected void addDictionary(org.jdiameter.client.impl.helpers.Parameters name, Node node) {
      AppConfiguration dicConfiguration = getInstance();

      Node param = node.getAttributes().getNamedItem("class");
      if(param != null) {
        String clazz = param.getNodeValue();
        dicConfiguration.add(DictionaryClass, clazz);
      }

      param =  node.getAttributes().getNamedItem("enabled");
      if(param != null) {
        String enabled = param.getNodeValue();
        dicConfiguration.add(DictionaryEnabled, Boolean.valueOf(enabled));
      }
      
      param =  node.getAttributes().getNamedItem("sendLevel");
      if(param != null) {
        String sendLevel = param.getNodeValue();
        dicConfiguration.add(DictionarySendLevel, sendLevel);
      }
      
      param =  node.getAttributes().getNamedItem("receiveLevel");
      if(param != null) {
        String receiveLevel = param.getNodeValue();
        dicConfiguration.add(DictionaryReceiveLevel, receiveLevel);
      }
      
      add(name, dicConfiguration);
    }

    protected void appendOtherParameter(Node node) {
    }

	protected void addThreadPool(Node item) {

		AppConfiguration threadPoolConfiguration = EmptyConfiguration.getInstance();
		NamedNodeMap attributes = item.getAttributes();

		for (int index = 0; index < attributes.getLength(); index++) {
			Node n = attributes.item(index);

			int v = Integer.parseInt(n.getNodeValue());
			if (n.getNodeName().equals("size")) {
				threadPoolConfiguration.add(ThreadPoolSize, v);
			} else if (n.getNodeName().equals("priority")) {
				threadPoolConfiguration.add(ThreadPoolPriority, v);
			} else {
				//log.error("Unkonwn attribute on " + item.getNodeName() + ", attribute name: " + n.getNodeName());
			}
		}
		if (!threadPoolConfiguration.isAttributeExist(ThreadPoolSize.ordinal())) {
			threadPoolConfiguration.add(ThreadPoolSize, ThreadPoolSize.defValue());
		}
		if (!threadPoolConfiguration.isAttributeExist(ThreadPoolPriority.ordinal())) {
			threadPoolConfiguration.add(ThreadPoolPriority, ThreadPoolPriority.defValue());
		}
		this.add(ThreadPool, threadPoolConfiguration);
	}

    protected void addSecurity(Node node) {
        NodeList c = node.getChildNodes();
        List<Configuration> items = new ArrayList<Configuration>();
        for (int i = 0; i < c.getLength(); i++) {
            String nodeName = c.item(i).getNodeName();
            if (nodeName.equals("SecurityData"))
                items.add(addSecurityData(c.item(i)));
        }
        add(Security, items.toArray(EMPTY_ARRAY));
    }

    protected Configuration addSecurityData(Node node) {
        AppConfiguration sd = getInstance().
            add(
                SDName,
                node.getAttributes().getNamedItem("name").getNodeValue()
            ).add(
                SDProtocol,
                node.getAttributes().getNamedItem("protocol").getNodeValue()
            ).add(
                SDEnableSessionCreation,
                Boolean.valueOf(node.getAttributes().getNamedItem("enable_session_creation").getNodeValue())
            ).add(
                SDUseClientMode,
                Boolean.valueOf(node.getAttributes().getNamedItem("use_client_mode").getNodeValue())
            );          
        NodeList c = node.getChildNodes();
        for (int i = 0; i < c.getLength(); i++) {
            Node cnode = c.item(i);
            String nodeName = cnode.getNodeName();
            if (nodeName.equals("CipherSuites"))
                sd.add(CipherSuites, cnode.getTextContent().trim());
            if (nodeName.equals("KeyData"))
                sd.add(
                    KeyData,
                    getInstance().
                            add(KDManager, cnode.getAttributes().getNamedItem("manager").getNodeValue()).
                            add(KDStore, cnode.getAttributes().getNamedItem("store").getNodeValue()).
                            add(KDFile, cnode.getAttributes().getNamedItem("file").getNodeValue()).
                            add(KDPwd, cnode.getAttributes().getNamedItem("pwd").getNodeValue())
                );
            if (nodeName.equals("TrustData"))
                sd.add(
                    TrustData,
                    getInstance().
                            add(TDManager, cnode.getAttributes().getNamedItem("manager").getNodeValue()).
                            add(TDStore, cnode.getAttributes().getNamedItem("store").getNodeValue()).
                            add(TDFile, cnode.getAttributes().getNamedItem("file").getNodeValue()).
                            add(TDPwd, cnode.getAttributes().getNamedItem("pwd").getNodeValue())
                );
        }
        return sd;
    }

    protected void addNetwork(Node node) {
        NodeList c = node.getChildNodes();
        for (int i = 0; i < c.getLength(); i++) {
            String nodeName = c.item(i).getNodeName();
            if (nodeName.equals("Peers")) addPeers(c.item(i));
            else if (nodeName.equals("Realms")) addRealms(c.item(i));
        }
    }

    protected void addPeers(Node node) {
        NodeList c = node.getChildNodes();
        ArrayList<Configuration> items = new ArrayList<Configuration>();
        for (int i = 0; i < c.getLength(); i++) {
            String nodeName = c.item(i).getNodeName();
            if (nodeName.equals("Peer"))
                items.add(addPeer(c.item(i)));
        }
        add(PeerTable, items.toArray(EMPTY_ARRAY));
    }

    protected void addRealms(Node node) {
        NodeList c = node.getChildNodes();
        ArrayList<Configuration> items = new ArrayList<Configuration>();
        for (int i = 0; i < c.getLength(); i++) {
            String nodeName = c.item(i).getNodeName();
            if (nodeName.equals("Realm"))
                items.add(addRealm(c.item(i)));
        }
        add(RealmTable, items.toArray(EMPTY_ARRAY));
    }

    protected Configuration addPeer(Node node) {
      AppConfiguration peerConfig = getInstance()
          .add(PeerRating, new Integer(node.getAttributes().getNamedItem("rating").getNodeValue()))
          .add(PeerName, node.getAttributes().getNamedItem("name").getNodeValue());
      if (node.getAttributes().getNamedItem("ip") != null) {
          peerConfig.add(PeerIp, node.getAttributes().getNamedItem("ip").getNodeValue());
      }
      if (node.getAttributes().getNamedItem("portRange") != null) {
          peerConfig.add(PeerLocalPortRange, node.getAttributes().getNamedItem("portRange").getNodeValue());
      }
      if (node.getAttributes().getNamedItem("security_ref") != null) {
          peerConfig.add(SecurityRef, node.getAttributes().getNamedItem("security_ref").getNodeValue());
      }
      
      return peerConfig;
    }

    protected Configuration addRealm(Node node) {
        return getInstance().
            add(
                RealmEntry,
                node.getAttributes().getNamedItem("name").getNodeValue() + ":" +  node.getAttributes().getNamedItem("peers").getNodeValue()
            );
    }

    protected void addExtensions(Node node) {
    }

    protected Long getLongValue(Node node) {
        return new Long(getValue(node));
    }

    protected Integer getIntValue(Node node) {
        return new Integer(getValue(node));
    }

    protected String getValue(Node node) {
        return node.getAttributes().getNamedItem("value").getNodeValue();
    }

    protected String getAttrValue(Node node, String name) {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }
}
