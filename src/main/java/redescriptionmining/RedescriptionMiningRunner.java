package redescriptionmining;

import si.ijs.kt.clus.Clus;
import si.ijs.kt.clus.algo.ClusInductionAlgorithmType;
import si.ijs.kt.clus.algo.rules.ClusRuleClassifier;
import si.ijs.kt.clus.algo.tdidt.tune.CDTTuneFTest;
import si.ijs.kt.clus.ext.ensemble.ClusEnsembleClassifier;
import si.ijs.kt.clus.main.settings.Settings;
import si.ijs.kt.clus.util.ClusLogger;
import si.ijs.kt.clus.util.exception.ClusException;
import si.ijs.kt.clus.util.jeans.util.cmdline.CMDLineArgs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class RedescriptionMiningRunner extends Clus {

	private String[] args;
	private ClusInductionAlgorithmType clss;
	private CMDLineArgs cargs;

	public RedescriptionMiningRunner(String[] args, DataSetCreator dataSetCreator, Settings settings) {
		
		this.args = args;
		setSettings(settings);
		
    	try {
			clss = initializeRunner(0, 0, 0, 0, null, 0, dataSetCreator, false);
		} catch (IOException | ClusException e) {
			e.printStackTrace();
		}
	}
	
	public RedescriptionMiningRunner(String[] args, int view, int W2indexStart, int W2indexEnd, int numAttr,
			ApplicationSettings appset, int initial, DataSetCreator dataSetCreator) {
		
		this.args = args;
		
    	try {
			clss = initializeRunner(view, W2indexStart, W2indexEnd, numAttr, appset, initial, dataSetCreator, true);
		} catch (IOException | ClusException e) {
			e.printStackTrace();
		}
    	
	}

	public static void main(String[] args) {
//		String[] argumentsTest = new String[] { "-forest", "view1.s" };
//		RedescriptionMiningRunner runner = new RedescriptionMiningRunner(argumentsTest);
		// HashMap<String, ArrayList<String>> map = runner.getRulesAndCoveredExamples();
		// System.out.println(map);
	}

	public ClusInductionAlgorithmType initializeRunner(int view, int W2indexStart, int W2indexEnd, int numAttr,
			ApplicationSettings appset, int initial, DataSetCreator dataSetCreator, boolean initSettings) throws IOException, ClusException {
		getSettings().setRedescriptionMining(true);
		cargs = new CMDLineArgs(this);
		cargs.process(args);

		if (cargs.allOK()) {
			getSettings().getGeneric().setDate(new Date());
			getSettings().getGeneric().setAppName(cargs.getMainArg(0));

			setSchema(dataSetCreator.schema);
			setData(dataSetCreator.data);

			if(initSettings) {
				initSettingsRedescriptionMining(cargs, view, W2indexStart, W2indexEnd, numAttr, appset, initial);
			}
			
			getSchema().setSettings(getSettings());
			
			// initSettings(cargs);

			// ClusLogger.initialize(getSettings().getGeneral()); // initialization of
			// logging.

			ClusInductionAlgorithmType clss = null;

			if (cargs.hasOption("rules")) {
				getSettings().getBeamSearch().setSectionBeamEnabled(true);
				getSettings().getRules().setSectionRulesEnabled(true);
				clss = new ClusRuleClassifier(this);
			}

			if (cargs.hasOption("forest")) {
				getSettings().getEnsemble().setEnsembleMode(true);
				clss = new ClusEnsembleClassifier(this);
				if (getSettings().getTree().getFTestArray().isVector()) {
					clss = new CDTTuneFTest(clss, getSettings().getTree().getFTestArray().getDoubleVector());
				}
			}

			initializeRedescriptionMining(cargs, clss);

			return clss;
		}
		return null;
	}

	public ArrayList<Rule> getRulesAndCoveredExamples(boolean settInit, Mappings map, DataSetCreator dat, ApplicationSettings appset) {
		setMap(map);
		setDat(dat);
		setAppset(appset);
		
		try {
	    	if(settInit) {
	    		if (cargs.hasOption("forest")) {
					getSettings().getEnsemble().setEnsembleMode(true);
					clss = new ClusEnsembleClassifier(this);
					if (getSettings().getTree().getFTestArray().isVector()) {
						clss = new CDTTuneFTest(clss, getSettings().getTree().getFTestArray().getDoubleVector());
					}
	    		}
	    		
	    		getSchema().setSettings(getSettings());
	    		getSettings().getGeneric().setDate(new Date());
				getSettings().getGeneric().setAppName(cargs.getMainArg(0));
				getSettings().initNamedValues();
				
				if (cargs != null) {
					getSettings().process(cargs);
		        }
				
				getSettings().getHMLC().initHierarchical();
	        	initializeRedescriptionMining(cargs, clss);
        	}

            singleRun(clss);

            ClusLogger.info("Done.");
            
	        } catch (Exception ex) {
	        	ex.printStackTrace();
	        }

	        return getRules();
	}
	
	public HashMap<String, ArrayList<String>> getRulesAndCoveredExamples(boolean settInit) {
        try {
        	if(settInit) {
        		
        		if (cargs.hasOption("forest")) {
    				getSettings().getEnsemble().setEnsembleMode(true);
    				clss = new ClusEnsembleClassifier(this);
    				if (getSettings().getTree().getFTestArray().isVector()) {
    					clss = new CDTTuneFTest(clss, getSettings().getTree().getFTestArray().getDoubleVector());
    				}
        		}
        		
        		getSchema().setSettings(getSettings());
        		getSettings().getGeneric().setDate(new Date());
    			getSettings().getGeneric().setAppName(cargs.getMainArg(0));
    			getSettings().initNamedValues();
    			
    			if (cargs != null) {
    				getSettings().process(cargs);
    	        }
    			
    			getSettings().getHMLC().initHierarchical();
            	initializeRedescriptionMining(cargs, clss);

        	}

            singleRun(clss);

            ClusLogger.info("Done.");
            
        } catch (Exception ex) {
        	ex.printStackTrace();
        }

        return getRedescriptionMiningRules();

    }

}
