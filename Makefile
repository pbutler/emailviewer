CLASSPATH=prefuse.jar:.:profusians.jar
JFLAGS= -cp $(CLASSPATH) 

JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

SOURCES = org/json/Cookie.java        \
          org/json/CDL.java           \
          org/json/CookieList.java    \
          org/json/HTTP.java          \
          org/json/HTTPTokener.java   \
          org/json/JSONArray.java     \
          org/json/JSONException.java \
          org/json/JSONML.java        \
          org/json/JSONString.java    \
          org/json/JSONStringer.java  \
          org/json/JSONObject.java    \
          org/json/JSONWriter.java    \
          org/json/JSONTokener.java   \
          org/json/XML.java           \
          org/json/Test.java          \
          org/json/XMLTokener.java 


SOURCES += edu/vt/Miner/SocialViewer.java \
	  edu/vt/Miner/RadialGraphView.java \
	  edu/vt/Miner/ForceDirectedView.java \
	  edu/vt/Miner/GraphView.java \
	  edu/vt/Miner/FlexibleZone.java \
	  edu/vt/Miner/FlexibleZoneFactory.java

all: SocialViewer.jar
	
CLASSES = $(SOURCES:.java=.class)



SocialViewer.jar: dump manifest.txt $(CLASSES)
	jar cfm SocialViewer.jar manifest.txt dump edu/vt/Miner/*.class org/json/*.class

empty: manifest.txt $(CLASSES)
	jar cfm SocialViewer.jar manifest.txt edu/vt/Miner/*.class org/json/*.class
clean:
	rm -f edu/vt/Miner/*.class org/json/*.class
	rm -f SocialViewer.jar
