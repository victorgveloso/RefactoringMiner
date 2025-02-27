package org.antlr.v4.tool;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.InterpreterRuleContext;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ATNSerializer;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.StarLoopEntryState;
import org.antlr.v4.runtime.tree.Trees;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

/** A heavier weight {@link ParserInterpreter} that creates parse trees
 *  that track alternative numbers for subtree roots.
 *
 * @since 4.5.1
 *
 */
public class GrammarParserInterpreter extends ParserInterpreter {
	/** The grammar associated with this interpreter. Unlike the
	 *  {@link ParserInterpreter} from the standard distribution,
	 *  this can reference Grammar, which is in the tools area not
	 *  purely runtime.
	 */
	protected final Grammar g;

	protected BitSet decisionStatesThatSetOuterAltNumInContext;

	/** Cache {@link LeftRecursiveRule#getPrimaryAlts()} and
	 *  {@link LeftRecursiveRule#getRecursiveOpAlts()} for states in
	 *  {@link #decisionStatesThatSetOuterAltNumInContext}. It only
	 *  caches decisions in left-recursive rules.
	 */
	protected int[][] stateToAltsMap;

	public GrammarParserInterpreter(Grammar g,
									String grammarFileName,
									Vocabulary vocabulary,
									Collection<String> ruleNames,
									ATN atn,
									TokenStream input) {
		super(grammarFileName, vocabulary, ruleNames, atn, input);
		this.g = g;
	}

	public GrammarParserInterpreter(Grammar g, ATN atn, TokenStream input) {
		super(g.fileName, g.getVocabulary(),
			  Arrays.asList(g.getRuleNames()),
			  atn, // must run ATN through serializer to set some state flags
			  input);
		this.g = g;
		decisionStatesThatSetOuterAltNumInContext = findOuterMostDecisionStates();
		stateToAltsMap = new int[g.atn.getNumberOfDecisions()][];
	}

	@Override
	protected InterpreterRuleContext createInterpreterRuleContext(ParserRuleContext parent,
																  int invokingStateNumber,
																  int ruleIndex)
	{
		return new GrammarInterpreterRuleContext(parent, invokingStateNumber, ruleIndex);
	}

	@Override
	public void reset() {
		super.reset();
		overrideDecisionRoot = null;
	}

	/** identify the ATN states where we need to set the outer alt number.
	 *  For regular rules, that's the block at the target to rule start state.
	 *  For left-recursive rules, we track the primary block, which looks just
	 *  like a regular rule's outer block, and the star loop block (always
	 *  there even if 1 alt).
	 */
	public BitSet findOuterMostDecisionStates() {
		BitSet track = new BitSet(atn.states.size());
		int numberOfDecisions = atn.getNumberOfDecisions();
		for (int i = 0; i < numberOfDecisions; i++) {
			DecisionState decisionState = atn.getDecisionState(i);
			RuleStartState startState = atn.ruleToStartState[decisionState.ruleIndex];
			// Look for StarLoopEntryState that is in any left recursive rule
			if ( decisionState instanceof StarLoopEntryState) {
				StarLoopEntryState loopEntry = (StarLoopEntryState)decisionState;
				if ( loopEntry.isPrecedenceDecision ) {
					// Recursive alts always result in a (...)* in the transformed
					// left recursive rule and that always has a BasicBlockStartState
					// even if just 1 recursive alt exists.
					ATNState blockStart = loopEntry.transition(0).target;
					// track the StarBlockStartState associated with the recursive alternatives
					track.set(blockStart.stateNumber);
				}
			}
			else if ( startState.transition(0).target == decisionState ) {
				// always track outermost block for any rule if it exists
				track.set(decisionState.stateNumber);
			}
		}
		return track;
	}

	/** Override this method so that we can record which alternative
	 *  was taken at each decision point. For non-left recursive rules,
	 *  it's simple. Set decisionStatesThatSetOuterAltNumInContext
	 *  indicates which decision states should set the outer alternative number.
	 *
	 *  Left recursive rules are much more complicated to deal with:
	 *  there is typically a decision for the primary alternatives and a
	 *  decision to choose between the recursive operator alternatives.
	 *  For example, the following left recursive rule has two primary and 2
	 *  recursive alternatives.</p>
	 *
		 e : e '*' e
		   | '-' INT
		   | e '+' e
		   | ID
		   ;

	 *  <p>ANTLR rewrites that rule to be</p>

		 e[int precedence]
			 : ('-' INT | ID)
			 ( {...}? '*' e[5]
			 | {...}? '+' e[3]
			 )*
			;

	 *
	 *  <p>So, there are two decisions associated with picking the outermost alt.
	 *  This complicates our tracking significantly. The outermost alternative number
	 *  is a function of the decision (ATN state) within a left recursive rule and the
	 *  predicted alternative coming back from adaptivePredict().
	 *
	 *  We use stateToAltsMap as a cache to avoid expensive calls to
	 *  getRecursiveOpAlts().
	 */
	@Override
	protected int visitDecisionState(DecisionState p) {
		int predictedAlt = super.visitDecisionState(p);
		if( p.getNumberOfTransitions() > 1) {
//			System.out.println("decision "+p.decision+": "+predictedAlt);
			if( p.decision == this.overrideDecision &&
				this._input.index() == this.overrideDecisionInputIndex )
			{
				overrideDecisionRoot = (GrammarInterpreterRuleContext)getContext();
			}
		}

		GrammarInterpreterRuleContext ctx = (GrammarInterpreterRuleContext)_ctx;
		if ( decisionStatesThatSetOuterAltNumInContext.get(p.stateNumber) ) {
			ctx.outerAltNum = predictedAlt;
			Rule r = g.getRule(p.ruleIndex);
			if ( atn.ruleToStartState[r.index].isLeftRecursiveRule ) {
				int[] alts = stateToAltsMap[p.stateNumber];
				LeftRecursiveRule lr = (LeftRecursiveRule) g.getRule(p.ruleIndex);
				if (p.getStateType() == ATNState.BLOCK_START) {
					if ( alts==null ) {
						alts = lr.getPrimaryAlts();
						stateToAltsMap[p.stateNumber] = alts; // cache it
					}
				}
				else if ( p.getStateType() == ATNState.STAR_BLOCK_START ) {
					if ( alts==null ) {
						alts = lr.getRecursiveOpAlts();
						stateToAltsMap[p.stateNumber] = alts; // cache it
					}
				}
				ctx.outerAltNum = alts[predictedAlt];
			}
		}

		return predictedAlt;
	}

	/** Given an ambiguous parse information, return the list of ambiguous parse trees.
	 *  An ambiguity occurs when a specific token sequence can be recognized
	 *  in more than one way by the grammar. These ambiguities are detected only
	 *  at decision points.
	 *
	 *  The list of trees includes the actual interpretation (that for
	 *  the minimum alternative number) and all ambiguous alternatives.
	 *  The actual interpretation is always first.
	 *
	 *  This method reuses the same physical input token stream used to
	 *  detect the ambiguity by the original parser in the first place.
	 *  This method resets/seeks within but does not alter originalParser.
	 *
	 *  The trees are rooted at the node whose start..stop token indices
	 *  include the start and stop indices of this ambiguity event. That is,
	 *  the trees returned will always include the complete ambiguous subphrase
	 *  identified by the ambiguity event.  The subtrees returned will
	 *  also always contain the node associated with the overridden decision.
	 *
	 *  Be aware that this method does NOT notify error or parse listeners as
	 *  it would trigger duplicate or otherwise unwanted events.
	 *
	 *  This uses a temporary ParserATNSimulator and a ParserInterpreter
	 *  so we don't mess up any statistics, event lists, etc...
	 *  The parse tree constructed while identifying/making ambiguityInfo is
	 *  not affected by this method as it creates a new parser interp to
	 *  get the ambiguous interpretations.
	 *
	 *  Nodes in the returned ambig trees are independent of the original parse
	 *  tree (constructed while identifying/creating ambiguityInfo).
	 *
	 *  @since 4.5.1
	 *
	 *  @param g              From which grammar should we drive alternative
	 *                        numbers and alternative labels.
	 *
	 *  @param originalParser The parser used to create ambiguityInfo; it
	 *                        is not modified by this routine and can be either
	 *                        a generated or interpreted parser. It's token
	 *                        stream *is* reset/seek()'d.
	 *  @param tokens		  A stream of tokens to use with the temporary parser.
	 *                        This will often be just the token stream within the
	 *                        original parser but here it is for flexibility.
	 *
	 *  @param decision       Which decision to try different alternatives for.
	 *
	 *  @param alts           The set of alternatives to try while re-parsing.
	 *
	 *  @param startIndex	  The index of the first token of the ambiguous
	 *                        input or other input of interest.
	 *
	 *  @param stopIndex      The index of the last token of the ambiguous input.
	 *                        The start and stop indexes are used primarily to
	 *                        identify how much of the resulting parse tree
	 *                        to return.
	 *
	 *  @param startRuleIndex The start rule for the entire grammar, not
	 *                        the ambiguous decision. We re-parse the entire input
	 *                        and so we need the original start rule.
	 *
	 *  @return               The list of all possible interpretations of
	 *                        the input for the decision in ambiguityInfo.
	 *                        The actual interpretation chosen by the parser
	 *                        is always given first because this method
	 *                        retests the input in alternative order and
	 *                        ANTLR always resolves ambiguities by choosing
	 *                        the first alternative that matches the input.
	 *                        The subtree returned
	 *
	 *  @throws RecognitionException Throws upon syntax error while matching
	 *                               ambig input.
	 */
	public static List<ParserRuleContext> getAllPossibleParseTrees(Grammar g,
																   Parser originalParser,
																   TokenStream tokens,
																   int decision,
																   BitSet alts,
																   int startIndex,
																   int stopIndex,
																   int startRuleIndex)
		throws RecognitionException
	{
		List<ParserRuleContext> trees = new ArrayList<ParserRuleContext>();
		// Create a new parser interpreter to parse the ambiguous subphrase
		ParserInterpreter parser;
		if (originalParser instanceof ParserInterpreter) {
			parser = new GrammarParserInterpreter(g, originalParser.getATN(), originalParser.getTokenStream());
		}
		else { // must've been a generated parser
			char[] serializedAtn = ATNSerializer.getSerializedAsChars(originalParser.getATN());
			ATN deserialized = new ATNDeserializer().deserialize(serializedAtn);
			parser = new ParserInterpreter(originalParser.getGrammarFileName(),
										   originalParser.getVocabulary(),
										   Arrays.asList(originalParser.getRuleNames()),
										   deserialized,
										   tokens);
		}

		parser.setInputStream(tokens);

		// Make sure that we don't get any error messages from using this temporary parser
		parser.setErrorHandler(new BailErrorStrategy());
		parser.removeErrorListeners();
		parser.removeParseListeners();
		parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);

		// get ambig trees
		int alt = alts.nextSetBit(0);
		while (alt >= 0) {
			// re-parse entire input for all ambiguous alternatives
			// (don't have to do first as it's been parsed, but do again for simplicity
			//  using this temp parser.)
			parser.reset();
			parser.getTokenStream().seek(0); // rewind the input all the way for re-parsing
			parser.addDecisionOverride(decision, startIndex, alt);
			ParserRuleContext t = parser.parse(startRuleIndex);
			GrammarInterpreterRuleContext ambigSubTree =
				(GrammarInterpreterRuleContext) Trees.getRootOfSubtreeEnclosingRegion(t, startIndex, stopIndex);
			// Use higher of overridden decision tree or tree enclosing all tokens
			if ( Trees.isAncestorOf(parser.getOverrideDecisionRoot(), ambigSubTree) ) {
				ambigSubTree = (GrammarInterpreterRuleContext)parser.getOverrideDecisionRoot();
			}
			trees.add(ambigSubTree);
			alt = alts.nextSetBit(alt + 1);
		}

		return trees;
	}

}
