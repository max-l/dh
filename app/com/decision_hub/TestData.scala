package com.decision_hub
import models.DecisionSummary

object TestData {
    
  val fakeDecisionSummaries = Seq(
      new DecisionSummary(
          1, "Za big Decision", "this is about the most important decision ever", 
          Seq(
            "Za best alternative" ->  454,
            "The most perfect alternatice" ->  142,
            "The alternatice of the enlightened" ->  33
          ),
          67,
          200, 163, 3
          ),
      new DecisionSummary(
          2, "Pizza or Indian ?", "no comments...", 
          Seq(
            "Pizza" ->  5,
            "Indian" ->  5
          ),
          89,
          26, 10, 6),
      new DecisionSummary(
          3, "Elect the master of the universe", "this will have consequences until the end of times !", 
          Nil,
          40,
          234234, 163, 334
          )
      )
}