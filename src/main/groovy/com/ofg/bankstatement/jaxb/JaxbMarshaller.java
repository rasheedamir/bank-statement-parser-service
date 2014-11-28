package com.ofg.bankstatement.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import java.io.OutputStream;
import java.io.StringWriter;

public class JaxbMarshaller {
    private JAXBContext jaxbContext;

    public JaxbMarshaller(String contextPath) {
        try {
            this.jaxbContext = JAXBContext.newInstance(contextPath);
        } catch (JAXBException e) {
            throw new RuntimeException("Error creating JAXB context!", e);
        }
    }

    @SuppressWarnings("rawtypes")
    public JaxbMarshaller(Class... classes) {
        try {
            this.jaxbContext = JAXBContext.newInstance(classes);
        } catch (JAXBException e) {
            throw new RuntimeException("Error creating JAXB context!", e);
        }
    }

    public void marshallObject(Object data, OutputStream outStream) {
        try {
            Marshaller marshaller = createMarshaller();
            marshaller.marshal(data, outStream);
        } catch (PropertyException e) {
            throw new RuntimeException(e);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public String marshallObjectToString(Object data) {
        try {
            Marshaller marshaller = createMarshaller();
            StringWriter sw = new StringWriter();
            marshaller.marshal(data, sw);
            return sw.toString();

        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private Marshaller createMarshaller() throws JAXBException {
        Marshaller marshaller = jaxbContext.createMarshaller();
        try {
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        } catch (PropertyException e) {
            throw new RuntimeException(e);
        }
        return marshaller;
    }
}
