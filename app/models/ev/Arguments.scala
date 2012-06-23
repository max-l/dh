package models.ev

object Arguments {

  def end2PartyDomination = 
    fr.custom(Argument(
      "Mettre fin au <u>système à deux partis</u>",
      "Ce n'est pas un hazard si toutes les démocraties qui utilisent un système de vote uninominal élisent toujours les mêmes deux partis pendant des décénies, parfois des siècles,",
      "il s'agit d'une défectuosité mathématique de ce type de système (démontré par Maurice Duvergé en 1950). " +
      "Le <b>vote évaluatif</b> s'il était instauré pourrait briser ce duopole."
    ))

  def solveVoteDivision = 
    fr.custom(Argument(
       "Voter sans craindre la <u>division du vote</u>",
       "À chaque élection, de nombreux électeurs ne votent pas pour l'option qu’ils préfèrent, par crainte de contribuer à la victoire d'un parti dont il craignent la prise de pouvoir.",
       """
        Ils votent stratégiquement, c'est à dire pour un parti qu'ils n’approuvent pas nécéssairement, mais
        qu’ils considèrent être "moins pire" que le parti qu'ils craignent, et que les sondages prédisent être 
        le plus susceptible de gagner. Un électeur qui est contraint de voter stratégiquement, perd en quelque 
        sorte le droit d’exprimer son intention réelle. Avec le vote évaluatif, l'électeur n'est jamais 
        pénalisé en votant selon ses convictions.
       """
    ))

   def endSimpleMajorityDictatorsihp =
     fr.custom(Argument(
       "Abolir la dictature de la majorité simple", //"Une antidote contre l'extremisme", //"Pour des partis politiques redevables à l'ensemble des électeurs et non uniquement à leur supporteurs",
       """
        Une majorité simple suffit pour gagner une élection, même lorsque celle ci constitue une minorité absolue.
        Le vote uninominal élit fréquement des partis qui sont fortement rejetés par une <b>majorité perdante</b>,
       """,
       """
        Le vote évaluatif change radicalement la donne : un parti détesté par une majorité paie un prix beaucoup plus élevé.
        Inversement, un parti qui fait consensus est récompensé proportionnelement au nombre d'électeurs qui partage
        ce consensus, et à l'intensité avec laquelle ils y adhèrent.
       """
     ))

  def solveVoteDivision0 =
     fr("Pour pouvoir voter selon ses convictions sans craindre la division du vote", """
        À chaque élection, de nombreux électeurs ne votent pas pour l'option qu’ils préfèrent, par crainte de 
        contribuer à la victoire d'un parti dont il craignent la prise de pouvoir. Ils votent stratégiquement, 
        c'est à dire pour un parti qu'ils n’approuvent pas nécéssairement, mais
        qu’ils considère être "moins pire" que le parti qu'ils craignent, et que les sondages prédisent être 
        le plus susceptible de gagner. Un électeur qui est contraint de voter stratégiquement, perd en quelque 
        sorte le droit d’exprimer son intention réelle. Avec le vote évaluatif, l'électeur n'est jamais 
        pénalisé en votant selon ses convictions.
        """) ~
     en("So that honnest voting is not penalized","""
        Many electors don't vote according to their true intents, by fear of contributing to elect an unwanted
        candidate, the are forced to vote for _strategically_ for the candidate that they don't necessarily like,
        but that is "not as bad" as the candidate they fear, and who is deemed electable by surveys.
        A voter that is forced into strategic voting looses is right to express his real intent. With approval voting
        one can vote honestly without penalty.
        """)

   def evaluateAllParties =
     fr("Pour que chaque parti soit soumis à l'évaluation de chaque électeur","""
        La libre expression est essentielle à la démocratie. L’élection est l’ultime exercice de ce droit d’expression, 
        car c’est le seul moment où elle a un impact décisif.
        Un système électoral doit être évalué sur sa capacité à véhiculer l’intention de l’électeur. Sur ce plan, 
        le mode de scrutin uninominal est extrèmement faible : il force l’électeur à donner un appui complet à un seul 
        candidat, et un rejet total de tous les autres. Le vote évaluatif, par sa plus grande expressivité, 
        donne à l’électeur le pouvoir qui lui est du.
        """) ~
     en("To give electors a change to evaluate each option *independently*","""
        Freedom of expression is an essential part of democracy. An election is the ultimate exercise of this right,
        it is the only time that a citizen's expression has a direct effect on the power by which he is governed.
        A voting method is only as good as it's capacity to communicate the intentions of the electors.
        Uninominal voting systems are extremely poor in that regard : it forces an elector to give the maximum
        approval to a single option, and reject all others equally. The expressiveness of an Approval Vote gives
        the elector the power that is rightfuly his.
        """)

   def aSickDemocracyWillDecline =
     fr("Parce qu’une démocratie malade crée les conditions de son déclin","""
        Entre la démocratie idéale et la dictature, il y a une grande zone grise. Il est plus exact de parler de santé 
        démocratique simplement la déclarer absente ou présente. Une démocratie est malade lorsqu’un groupe d'élus réeussent à 
        accroître leur pouvoirs au delà de ce que lui accorde l’électorat. Une telle situation crée de nombreuses occasions 
        permettant à ce groupe de consolider son pouvoir au détriment de l’électorat, fermant ainsi la boucle d’un cercle vicieux. 
        Le vote évaluatif, en rétablissant l’équilibre en faveur des électeurs, 
        permettrait d’inverser le déclin démocratique actuel.
        """) ~
     en("Because a weakened democracy creates the conditions for it's decline","""
        Between democracy and dictatorship, there is a wide gray area. It is more precise to speak of democratic health rather than
        it's presense or absence. A democracy is weakend when politicians obtain more power than those granted by the electorate,
        it is a slipery slope : these politicians can use powers to obtain more powers, closing the loop of a vicous circle.
        Evaluative Voting could help invert this democratic decline.
        """)  
}