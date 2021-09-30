package de.uni.tuebingen.sfs.clarind.core;

import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.tc.api.*;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusStored;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class Conllu2tcfConverter {

    String lang;

    public Conllu2tcfConverter(String lang) {

        /**
         * Convert CoNNL-U to TCF file
         * @param input an InputStream that should represent a conllu document
         * @param output OutputStream for TCF file
         * @throws WLFormatException
         */
        this.lang = lang;

    }

    /**
     * Convert CoNNL-U to TCF file
     *
     * @param input  stream of CoNNL-U file
     * @param output OutputStream for TCF file
     * @throws WLFormatException
     */
    public TextCorpusStored convert(InputStream input, OutputStream output) throws WLFormatException {


        // create Token and Sentence layer
        StringBuilder rawText = new StringBuilder();
        TextCorpusStored textCorpus = new TextCorpusStored(this.lang);
        TokensLayer tokensLayer = textCorpus.createTokensLayer();
        SentencesLayer sentencesLayer = textCorpus.createSentencesLayer();
        LemmasLayer lemmasLayer = null;
        PosTagsLayer posLayer = null;
        MorphologyLayer morphologyLayer = null;
        DependencyParsingLayer dependencyParsingLayer = null;
        List<DependencyRelation> depRelations = new ArrayList<>();

        int index = 0;
        int k = 0;
        String surfaceForm = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            ArrayList<Token> sentence = new ArrayList<>();
            Token token;
            String[] parts = null;
            String line;


            while ((line = reader.readLine()) != null) {

                if (line.startsWith("# text = ")) {
                    rawText.append(line.substring(line.indexOf('=') + 1));
                }

                // skip comments in .conllu
                if (line.startsWith("#")) {
                    continue;
                }

                // add sentence to sentenceLayer when empty line occurs
                if (line.length() == 0) {
                    if (sentence.size() > 0) {
                        sentencesLayer.addSentence(sentence);
                    }
                    sentence.clear();
                    continue;
                }

                String[] columns = line.split("\\s+");

                // get multi-token
                if (columns[0].contains("-")) {
                    String[] multiIndeces = columns[0].split("-");
                    int index1 = Integer.parseInt(multiIndeces[0]);
                    int index2 = Integer.parseInt(multiIndeces[1]);
                    k = index2 - index1 + 1;

                    surfaceForm = columns[1];
                    parts = multiTokenParts(k, tokensLayer.size());
                    continue;
                }

                if (k > 0) {
                    token = tokensLayer.addTokenWithSurfaceFormParts(columns[1], surfaceForm, parts);
                    sentence.add(token);
                    k--;
                } else {
                    token = tokensLayer.addToken(columns[1]); // word form
                    sentence.add(token);
                }

                // fill Lemma layer if available
                if (!columns[2].equals("_")) {
                    if (lemmasLayer == null) {
                        lemmasLayer = textCorpus.createLemmasLayer();
                    }
                    lemmasLayer.addLemma(columns[2], token);
                }

                // fill POS layer if available
                if (!columns[3].equals("_")) {
                    if (posLayer == null) {
                        posLayer = textCorpus.createPosTagsLayer("universal-pos");
                    }
                    posLayer.addTag(columns[3], token);
                }

                // fill morphological layer if available
                if (!columns[5].equals("_")) {
                    if (morphologyLayer == null) {
                        morphologyLayer = textCorpus.createMorphologyLayer("UD");
                    }
                    String[] featureList = columns[5].split("\\|");
                    List<Feature> fList = new ArrayList<>();
                    for (String feature : featureList) {
                        String[] typeValue = feature.split("=");

                        // in case value is a list, add new tags for all of them
                        if (typeValue[1].contains(",")) {
                            String[] values = typeValue[1].split(",");
                            for (String value : values) {
                                Feature f = morphologyLayer.createFeature(typeValue[0], value);
                                fList.add(f);
                            }
                        } else {
                            Feature f = morphologyLayer.createFeature(typeValue[0], typeValue[1]);
                            fList.add(f);
                        }
                    }
                    morphologyLayer.addAnalysis(token, fList);
                }

                // collect information for dependency layere
                // if DEPS filled out, no need to pay attention to HEAD and DEP_REL as DEPS
                // contains these informations already
                if (!columns[8].equals("_")) {
                    if (dependencyParsingLayer == null) {
                        dependencyParsingLayer = textCorpus.createDependencyParsingLayer("universal-dep", true, true);
                    }
                    String[] allDependencies = columns[8].split("\\|");
                    for (String headDepPair : allDependencies) {
                        int head = Integer.parseInt(headDepPair.split(":")[0]);
                        String func = headDepPair.split(":")[1];

                        int headIndex = Math.abs(head - Integer.parseInt(columns[0]) + index);
                        DependencyRelation depRel = new DependencyRelation(token, func,
                                Integer.parseInt(columns[0]), headIndex, sentencesLayer.size());
                        depRelations.add(depRel);
                    }

                } else if (!columns[6].equals("_") && !columns[7].equals("_")) {
                    if (dependencyParsingLayer == null) {
                        dependencyParsingLayer = textCorpus.createDependencyParsingLayer("universal-dep", false, true);
                    }

                    // get govID by getting the difference between curr and head node + overall index of values
                    int headIndex = Math.abs(Integer.parseInt(columns[6]) - Integer.parseInt(columns[0]) + index);
                    // save current Dependency Relation, since some Head Tokens might not have been parsed yet
                    DependencyRelation depRel = new DependencyRelation(token, columns[7],
                            Integer.parseInt(columns[0]), headIndex, sentencesLayer.size());
                    depRelations.add(depRel);
                }
                index++;
            }

            // in case the .conllu doesn't end with an empty line
            if (sentence.size() > 0) {
                sentencesLayer.addSentence(sentence);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // build dependency layer
        if (dependencyParsingLayer != null) {
            int prevSentenceID = 0;
            List<Dependency> dependencies = new ArrayList<>();
            for (DependencyRelation depRelation : depRelations) {
                if (prevSentenceID != depRelation.getSentenceID()) {
                    dependencyParsingLayer.addParse(dependencies);
                    dependencies.clear();
                    prevSentenceID = depRelation.getSentenceID();
                }

                Dependency dep;
                if (depRelation.getRelationType().equals("root")) {
                    dep = dependencyParsingLayer.createDependency(depRelation.getRelationType(),
                            depRelation.getToken());
                } else {
                    dep = dependencyParsingLayer.createDependency(depRelation.getRelationType(),
                            depRelation.getToken(), tokensLayer.getToken(depRelation.getGovID()));
                }
                dependencies.add(dep);
            }
            dependencyParsingLayer.addParse(dependencies);
        }

        // build TextLayer if # text comments are found in the CoNLL-U
        if (rawText.length() > 0) {
            TextLayer textLayer = textCorpus.createTextLayer();
            textLayer.addText(rawText.toString());
        }
        return textCorpus;

    }

    private String[] multiTokenParts(int k, int tokenLayerLength) {
        String[] parts = new String[k];
        for (int i = 0; i < k; i++) {
            parts[i] = "t_" + tokenLayerLength;
            tokenLayerLength++;
        }
        return parts;
    }


}
