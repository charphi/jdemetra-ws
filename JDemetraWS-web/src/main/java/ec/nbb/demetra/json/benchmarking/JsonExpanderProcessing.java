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
package ec.nbb.demetra.json.benchmarking;

import ec.benchmarking.simplets.TsExpander;
import ec.nbb.demetra.json.JsonTsData;
import ec.nbb.demetra.json.JsonTsDomain;
import ec.tstoolkit.timeseries.TsAggregationType;

/**
 *
 * @author Mats Maggi
 */
public class JsonExpanderProcessing {

    public JsonTsData y;
    public int defaultFrequency; // only used when domain == null 
    public JsonTsDomain domain;
    public boolean useparam = false;
    public double parameter = .9;
    public boolean trend = false;
    public boolean constant = false;
    public TsExpander.Model model = TsExpander.Model.I1;

    public int differencing = 1; // >=1, <=5 
    public TsAggregationType agg = TsAggregationType.Sum;
}
