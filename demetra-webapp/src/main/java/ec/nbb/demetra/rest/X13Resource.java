/*
 * Copyright 2014 National Bank of Belgium
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
package ec.nbb.demetra.rest;

import com.google.common.base.Strings;
import ec.demetra.xml.sa.x13.X13XmlProcessor;
import ec.demetra.xml.sa.x13.XmlX13Request;
import ec.demetra.xml.sa.x13.XmlX13Requests;
import ec.nbb.demetra.Messages;
import ec.nbb.demetra.json.excel.ExcelSeries;
import ec.nbb.demetra.model.rest.utils.RestUtils;
import ec.nbb.ws.annotations.Compress;
import ec.satoolkit.algorithm.implementation.X13ProcessingFactory;
import ec.satoolkit.x13.X13Specification;
import ec.tss.TsCollection;
import ec.tss.TsCollectionInformation;
import ec.tss.TsFactory;
import ec.tss.TsInformation;
import ec.tss.TsInformationType;
import ec.tss.xml.XmlTsData;
import ec.tss.xml.x13.XmlX13Specification;
import ec.tstoolkit.algorithm.CompositeResults;
import ec.tstoolkit.timeseries.simplets.TsData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Mats Maggi
 */
@Path("/x13")
@Api(value = "/x13")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class X13Resource {

    private final String[] components = {"sa", "t", "s", "i", "y_f"};

    @POST
    @Compress
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(value = "Returns the components of the X13 processing of the given series", response = XmlTsData.class, responseContainer = "Map")
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "X13 was successfully processed", response = XmlTsData.class, responseContainer = "Map"),
                @ApiResponse(code = 400, message = "Bad request", response = String.class),
                @ApiResponse(code = 500, message = "Invalid request", response = String.class)
            }
    )
    public Response x13(
            @ApiParam(name = "tsData", required = true) XmlTsData tsData,
            @ApiParam(name = "spec", defaultValue = "RSA4c")
            @QueryParam(value = "spec") @DefaultValue("RSA4c") String spec) {
        CompositeResults results = null;
        X13Specification specification;
        Map<String, XmlTsData> compMap = new HashMap<>();

        if (Strings.isNullOrEmpty(spec)) {
            specification = X13Specification.RSA4;
        } else {
            specification = X13Specification.fromString(spec);
        }

        if (tsData == null) {
            throw new IllegalArgumentException(Messages.TS_NULL);
        } else {
            TsData data = tsData.create();
            if (data.isEmpty()) {
                throw new IllegalArgumentException(Messages.TS_EMPTY);
            }
            results = X13ProcessingFactory.process(data, specification);
        }

        if (results == null) {
            throw new IllegalArgumentException(Messages.PROCESSING_ERROR);
        } else {
            for (String c : components) {
                if (results.contains(c)) {
                    TsData compData = results.getData(c, TsData.class);
                    XmlTsData xml = new XmlTsData();
                    xml.copy(compData);
                    compMap.put(c, xml);
                }
            }
        }
        return Response.ok().entity(compMap).build();
    }
    
    @GET
    @Path("{spec}")
    @Compress
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(value = "Returns the specification schema from a given specification name", response = XmlX13Specification.class)
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "X13 specification schema was successfully returned", response = XmlX13Specification.class),
                @ApiResponse(code = 400, message = "Bad request", response = String.class),
                @ApiResponse(code = 500, message = "Invalid request", response = String.class)
            }
    )
    public Response x13Spec(@PathParam("spec") String spec) {
        X13Specification specification;
        if (Strings.isNullOrEmpty(spec)) {
            throw new IllegalArgumentException(String.format(Messages.UNKNOWN_SPEC, spec));
        } else {
            specification = X13Specification.fromString(spec);
        }
        
        XmlX13Specification xml = new XmlX13Specification();
        xml.copy(specification);
        
        return Response.ok().entity(xml).build();
    }
    
    @POST
    @Path("request")
    @Compress
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(value = "Returns the requested components of the X13 processing of the given series", response = ec.demetra.xml.core.XmlInformationSet.class)
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "X13 was successfully processed", response = ec.demetra.xml.core.XmlInformationSet.class),
                @ApiResponse(code = 400, message = "Bad request", response = String.class),
                @ApiResponse(code = 500, message = "Invalid request", response = String.class)
            }
    )
    public Response x13(@ApiParam(name = "request", required = true) XmlX13Request request) {
        X13XmlProcessor processor = new X13XmlProcessor();
        ec.demetra.xml.core.XmlInformationSet set = processor.process(request);
        if (set == null) {
            throw new IllegalArgumentException("Unable to process the request, please check your inputs.");
        }
        
        return Response.ok().entity(set).build();
    }
    
    @POST
    @Path("requests")
    @Compress
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(value = "Returns the requested components of the X13 processing of the given series", response = ec.demetra.xml.core.XmlInformationSet.class)
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "X13 was successfully processed", response = ec.demetra.xml.core.XmlInformationSet.class),
                @ApiResponse(code = 400, message = "Bad request", response = String.class),
                @ApiResponse(code = 500, message = "Invalid request", response = String.class)
            }
    )
    public Response x13(@ApiParam(name = "request", required = true) XmlX13Requests requests) {
        X13XmlProcessor processor = new X13XmlProcessor();
        ec.demetra.xml.core.XmlInformationSet set = processor.process(requests);
        if (set == null) {
            throw new IllegalArgumentException("Unable to process the request, please check your inputs.");
        }
        
        return Response.ok().entity(set).build();
    }
    
    @POST
    @Compress
    @Path("excel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returns the components of the X13 processing of the given series", response = ExcelSeries.class)
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "X13 was successfully processed", response = ExcelSeries.class),
                @ApiResponse(code = 400, message = "Bad request", response = String.class),
                @ApiResponse(code = 500, message = "Invalid request", response = String.class)
            }
    )
    public Response x13Excel(@ApiParam(name = "series", required = true) ExcelSeries series,
            @ApiParam(name = "spec") @QueryParam(value = "spec") String spec,
            @ApiParam(name = "components") @QueryParam(value = "components") List<String> components) {
        X13Specification specification;
        if (Strings.isNullOrEmpty(spec)) {
            specification = X13Specification.RSA5;
        } else {
            specification = X13Specification.fromString(spec);
        }

        TsCollectionInformation infoColl = RestUtils.readExcelSeries(series);
        CompositeResults[] results = new CompositeResults[infoColl.items.size()];
        if (infoColl == null) {
            throw new IllegalArgumentException(Messages.TS_NULL);
        } else {
            int i = 0;
            for (TsInformation info : infoColl.items) {
                results[i] = X13ProcessingFactory.process(info.data, specification);
                i++;
            }
        }

        TsCollection coll = TsFactory.instance.createTsCollection("results");
        
        if (results == null) {
            throw new IllegalArgumentException(Messages.PROCESSING_ERROR);
        } else {
            if (components == null || components.isEmpty()) {
                components = Arrays.asList(this.components);
            }
            
            int i = 0;
            for (CompositeResults result : results) {
                for (String c : components) {
                    if (result != null && result.contains(c)) {
                        TsData compData = result.getData(c, TsData.class);                        
                        coll.add(TsFactory.instance.createTs(c + " ("+ i++ + ")", null, compData));
                    } else {
                        coll.add(TsFactory.instance.createTs(c + " ("+ i++ + ")"));
                    }
                }
            }
        }
        
        // Format results
        ExcelSeries response = RestUtils.toExcelSeries(new TsCollectionInformation(coll, TsInformationType.Data));
        return Response.ok().entity(response).build();
    }
}
