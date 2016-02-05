/*
 * Copyright 2015 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package ec.nbb.demetra.rest.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import ec.nbb.demetra.model.balancing.Summary;
import io.swagger.util.Json;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import org.glassfish.grizzly.compression.zip.GZipDecoder;
import org.glassfish.grizzly.compression.zip.GZipFilter;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.glassfish.jersey.message.GZipEncoder;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mats Maggi
 */
public class BalancingTest {

    @Test
    public void balancingTest() {
        InputStream fs = null;
        GZIPInputStream stream = null;
        try {
            fs = new FileInputStream("R:\\RES\\TRICONAT\\QSUT\\constraints.gz");
            stream = new GZIPInputStream(fs);

            JerseyClientBuilder jcb = new JerseyClientBuilder();
            jcb.register(GZipDecoder.class);
            jcb.register(GZipEncoder.class);
            jcb.register(GZipFilter.class);
            JerseyClient jc = jcb.build();

            Stopwatch watch = Stopwatch.createStarted();
            JerseyWebTarget jwt = jc.target(TestConfig.getUrl());
            //JerseyWebTarget jwt = jc.target("https://pc0021770.nbb.local:8181/demetra/api"); // Needs installation of certificate
            Response resp = jwt.path("balancing")
                    .request(MediaType.APPLICATION_JSON)
                    .acceptEncoding("gzip")
                    .post(Entity.entity(stream, new Variant(MediaType.APPLICATION_JSON_TYPE, (String) null, "gzip")));

            Assert.assertEquals(200, resp.getStatus());

            Summary summary = resp.readEntity(Summary.class);

            Summary data = new Summary();
            data.setDimensions(summary.getDimensions());
            data.setDimensionsCount(summary.getDimensionsCount());
            data.setData(summary.getData());

            Summary constraints = new Summary();
            constraints.setConstraints(summary.getConstraints());
            constraints.setDimensions(summary.getDimensions());
            constraints.setDimensionsCount(summary.getDimensionsCount());
            
            Summary references = new Summary();
            references.setDimensions(summary.getDimensions());
            references.setDimensionsCount(summary.getDimensionsCount());
            references.setReferences(summary.getReferences());

            FileOutputStream fos = new FileOutputStream("R:\\RES\\TRICONAT\\QSUT\\Balancing\\Test\\Pretty Print\\constraints_2010.json");
            Json.pretty().writeValue(fos, constraints);
            fos.close();

            fos = new FileOutputStream("R:\\RES\\TRICONAT\\QSUT\\Balancing\\Test\\Pretty Print\\data_2010.json");
            Json.pretty().writeValue(fos, data);
            fos.close();
            
            fos = new FileOutputStream("R:\\RES\\TRICONAT\\QSUT\\Balancing\\Test\\Pretty Print\\references_2010.json");
            Json.pretty().writeValue(fos, references);
            fos.close();
        } catch (IOException | NullPointerException ex) {
            ex.printStackTrace();
            Logger.getLogger(BalancingTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fs != null) {
                    fs.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(BalancingTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    //@Test
    public void readFile() {
        GZIPInputStream stream = null;
        FileInputStream fs = null;
        try {
            fs = new FileInputStream("C:\\LocalData\\MAGGIMA\\yearly_summary.gz");
            stream = new GZIPInputStream(fs);
            BufferedInputStream bis = new BufferedInputStream(stream);
            Stopwatch stop = Stopwatch.createStarted();
            Summary summary = new ObjectMapper().readValue(bis, Summary.class);
            FileOutputStream fos = new FileOutputStream("C:\\LocalData\\MAGGIMA\\yearly_summary2.gz");
            byte[] bytes = Json.mapper().writeValueAsBytes(summary);

            GZIPOutputStream gzip = new GZIPOutputStream(fos);
            gzip.write(bytes);
            gzip.close();
            fos.close();
            System.out.println(stop.stop().elapsed(TimeUnit.SECONDS));
            System.out.println("Finished !");
        } catch (Exception ex) {
            Logger.getLogger(BalancingTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fs != null) {
                    fs.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(BalancingTest.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(BalancingTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}