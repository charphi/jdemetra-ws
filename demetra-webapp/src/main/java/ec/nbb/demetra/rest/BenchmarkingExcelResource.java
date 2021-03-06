/*
 * Copyright 2017 National Bank of Belgium
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

import ec.benchmarking.simplets.TsCholette;
import ec.benchmarking.simplets.TsExpander;
import ec.benchmarking.simplets.TsExpander.Model;
import ec.nbb.demetra.Messages;
import ec.nbb.demetra.benchmarking.TempDisaggOutput;
import ec.nbb.demetra.benchmarking.TempDisaggOutputJson;
import ec.nbb.demetra.benchmarking.TempDisaggSpec;
import static ec.nbb.demetra.benchmarking.TemporalDisaggregation.process;
import ec.nbb.demetra.json.excel.ExcelSeries;
import ec.nbb.demetra.model.rest.utils.DentonProcessing;
import ec.nbb.demetra.model.rest.utils.DentonSpecification;
import ec.nbb.demetra.model.rest.utils.RestUtils;
import ec.nbb.ws.annotations.Compress;
import ec.tss.TsCollection;
import ec.tss.TsCollectionInformation;
import ec.tss.TsFactory;
import ec.tss.TsInformationType;
import ec.tstoolkit.Parameter;
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.timeseries.TsAggregationType;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Mats Maggi
 */
@Path("/benchmarking/excel")
@Api(value = "/benchmarking/excel", hidden = false)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BenchmarkingExcelResource {

    @POST
    @Compress
    @Path("/denton")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Denton processing", notes = "Computes a Denton Processing on a given series", response = ExcelSeries.class)
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "Denton processing successfully done", response = ExcelSeries.class),
                @ApiResponse(code = 400, message = "Bad request", response = String.class),
                @ApiResponse(code = 500, message = "Invalid request", response = String.class)
            }
    )
    public Response dentonProcessing(@ApiParam(value = "series", required = true) ExcelSeries series,
            @ApiParam(name = "mul", defaultValue = "true") @QueryParam(value = "mul") @DefaultValue("true") boolean mul,
            @ApiParam(name = "modified", defaultValue = "true") @QueryParam(value = "modified") @DefaultValue("true") boolean modified,
            @ApiParam(name = "differencing", defaultValue = "1") @QueryParam(value = "differencing") @DefaultValue("1") int differencing,
            @ApiParam(name = "agg", defaultValue = "Sum") @QueryParam(value = "agg") @DefaultValue("Sum") TsAggregationType agg,
            @ApiParam(name = "freq", defaultValue = "0") @QueryParam(value = "freq") @DefaultValue("0") int freq) {
        TsCollectionInformation info = RestUtils.readExcelSeries(series);
        if (!info.hasData() || info.items.isEmpty()) {
            throw new IllegalArgumentException("1 or 2 series must be provided !");
        }

        DentonSpecification spec = new DentonSpecification();
        spec.setType(agg);
        spec.setDifferencing(differencing);
        spec.setModified(modified);
        spec.setMultiplicative(mul);

        TsData xbench = null;
        if (info.items.size() == 1) {
            xbench = DentonProcessing.process(info.items.get(0).data, TsFrequency.valueOf(freq), spec);
        } else if (info.items.size() == 2) {
            TsData sa = info.items.get(0).data;
            TsData raw = info.items.get(1).data;
            if (raw.getFrequency() != TsFrequency.Yearly) {
                raw = raw.changeFrequency(TsFrequency.Yearly, agg, true);
            }
            xbench = DentonProcessing.process(sa, raw, spec);
        }

        if (xbench != null) {
            TsCollection coll = TsFactory.instance.createTsCollection("results");
            coll.add(TsFactory.instance.createTs(info.items.get(0).name, null, xbench));

            return Response.ok()
                    .entity(RestUtils.toExcelSeries(new TsCollectionInformation(coll, TsInformationType.Data)))
                    .build();
        } else {
            throw new IllegalArgumentException(Messages.PROCESSING_ERROR);
        }
    }

    @POST
    @Compress
    @Path("/cholette")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Cholette processing", notes = "Computes a Cholette Processing on given series", response = ExcelSeries.class)
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "Cholette processing successfully done", response = ExcelSeries.class),
                @ApiResponse(code = 400, message = "Bad request", response = String.class),
                @ApiResponse(code = 500, message = "Invalid request", response = String.class)
            }
    )
    public Response choletteProcessing(@ApiParam(value = "series", required = true) ExcelSeries series,
            @ApiParam(name = "rho", defaultValue = "1") @QueryParam(value = "rho") @DefaultValue("1") double rho,
            @ApiParam(name = "lambda", defaultValue = "1") @QueryParam(value = "lambda") @DefaultValue("1") double lambda,
            @ApiParam(name = "bias", defaultValue = "None") @QueryParam(value = "bias") @DefaultValue("None") String bias,
            @ApiParam(name = "agg", defaultValue = "Sum") @QueryParam(value = "agg") @DefaultValue("Sum") TsAggregationType agg) {
        TsCholette cholette = new TsCholette();
        cholette.setAggregationType(agg);
        cholette.setRho(rho);
        cholette.setLambda(lambda);
        cholette.setBiasCorrection(TsCholette.BiasCorrection.valueOf(bias));

        TsCollectionInformation info = RestUtils.readExcelSeries(series);

        if (!info.hasData() || info.items.size() != 2) {
            throw new IllegalArgumentException("2 series must be provided !");
        }

        TsData xbench = cholette.process(info.items.get(0).data, info.items.get(1).data);

        if (xbench != null) {
            TsCollection coll = TsFactory.instance.createTsCollection("results");
            coll.add(TsFactory.instance.createTs(info.items.get(0).name, null, xbench));

            return Response.ok()
                    .entity(RestUtils.toExcelSeries(new TsCollectionInformation(coll, TsInformationType.Data)))
                    .build();
        } else {
            throw new IllegalArgumentException(Messages.PROCESSING_ERROR);
        }
    }

    @POST
    @Compress
    @Path("/expander")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Expander processing", notes = "Computes a Expander Processing on given series", response = ExcelSeries.class)
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "Expander processing successfully done", response = ExcelSeries.class),
                @ApiResponse(code = 400, message = "Bad request", response = String.class),
                @ApiResponse(code = 500, message = "Invalid request", response = String.class)
            }
    )
    public Response expanderProcessing(@ApiParam(value = "series", required = true) ExcelSeries series,
            @ApiParam(name = "defaultFreq") @QueryParam(value = "defaultFreq") int defaultFreq,
            @ApiParam(name = "useParam", defaultValue = "false") @QueryParam(value = "useParam") @DefaultValue("false") boolean useParam,
            @ApiParam(name = "parameter", defaultValue = "0.9") @QueryParam(value = "parameter") @DefaultValue("0.9") double parameter,
            @ApiParam(name = "trend", defaultValue = "false") @QueryParam(value = "trend") @DefaultValue("false") boolean trend,
            @ApiParam(name = "constant", defaultValue = "false") @QueryParam(value = "constant") @DefaultValue("false") boolean constant,
            @ApiParam(name = "model", defaultValue = "I1") @QueryParam(value = "model") @DefaultValue("I1") String model,
            @ApiParam(name = "agg", defaultValue = "Sum") @QueryParam(value = "agg") @DefaultValue("Sum") TsAggregationType agg) {
        TsExpander expander = new TsExpander();
        expander.setType(agg);
        expander.setModel(Model.valueOf(model));
        if (useParam) {
            expander.setParameter(parameter);
            expander.estimateParameter(false);
        } else {
            expander.estimateParameter(true);
        }
        expander.useConst(constant);
        expander.useTrend(trend);

        TsCollectionInformation infoColl = RestUtils.readExcelSeries(series);
        TsCollection coll = TsFactory.instance.createTsCollection("results");

        if (infoColl == null) {
            throw new IllegalArgumentException(Messages.TS_NULL);
        } else {
            infoColl.items.stream().forEach((info) -> {
                if (info.hasData()) {
                    TsData t = expander.expand(info.data, TsFrequency.valueOf(defaultFreq));
                    if (t == null) {
                        coll.add(TsFactory.instance.createTs(info.name));
                    } else {
                        coll.add(TsFactory.instance.createTs(info.name, null, t));
                    }
                } else {
                    coll.add(TsFactory.instance.createTs(info.name));
                }
            });
        }

        // Format results
        ExcelSeries response = RestUtils.toExcelSeries(new TsCollectionInformation(coll, TsInformationType.Data));
        return Response.ok().entity(response).build();
    }

    @POST
    @Compress
    @Path("/tempdisagg")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Temporal Disaggregation processing", notes = "Computes a temporal disaggregation on given series", response = ExcelSeries.class)
    @ApiResponses(
            value = {
                @ApiResponse(code = 200, message = "Temporal disaggregation successfully done", response = ExcelSeries.class),
                @ApiResponse(code = 400, message = "Bad request", response = String.class),
                @ApiResponse(code = 500, message = "Invalid request", response = String.class)
            }
    )
    public Response tempDisaggregation(@ApiParam(value = "series", required = true) Map<String, ExcelSeries> series,
            @ApiParam(name = "defaultFreq") @QueryParam(value = "defaultFreq") int defaultFreq,
            @ApiParam(name = "useParam", defaultValue = "false") @QueryParam(value = "useParam") @DefaultValue("false") boolean useParam,
            @ApiParam(name = "parameter", defaultValue = "0.9") @QueryParam(value = "parameter") @DefaultValue("0.9") double parameter,
            @ApiParam(name = "trend", defaultValue = "false") @QueryParam(value = "trend") @DefaultValue("false") boolean trend,
            @ApiParam(name = "constant", defaultValue = "true") @QueryParam(value = "constant") @DefaultValue("true") boolean constant,
            @ApiParam(name = "model", defaultValue = "Ar1") @QueryParam(value = "model") @DefaultValue("Ar1") TempDisaggSpec.Model model,
            @ApiParam(name = "agg", defaultValue = "Sum") @QueryParam(value = "agg") @DefaultValue("Sum") TsAggregationType agg) {
        TempDisaggSpec spec = new TempDisaggSpec();
        spec.setDefaultFrequency(TsFrequency.valueOf(defaultFreq));
        spec.setTrend(trend);
        spec.setConstant(constant);
        spec.setModel(model);
        if (useParam) {
            Parameter p = new Parameter(parameter, ParameterType.Fixed);
            spec.setParameter(p);
        }
        spec.setType(agg);

        if (!series.containsKey("input")) {
            throw new IllegalArgumentException("Map of series doesn't contain any input series");
        }

        TsCollectionInformation input = RestUtils.readExcelSeries(series.get("input"));
        TsData y = input.items.get(0).data;

        TsData[] x = null;
        if (series.containsKey("indicators")) {
            TsCollectionInformation indicators = RestUtils.readExcelSeries(series.get("indicators"));
            x = indicators.items.stream().map(tsInfo -> tsInfo.data).toArray(TsData[]::new);
        }

        TempDisaggOutput output = process(y, x, spec);
        if (output == null) {
            return Response.serverError().entity("Unable to get results !").build();
        } else {
            TempDisaggOutputJson json = new TempDisaggOutputJson(output);
            return Response.ok().entity(json).build();
        }
    }
}
