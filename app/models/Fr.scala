package models

object fr extends I18nLanguage {
  def code = "fr"
}
object en extends I18nLanguage {
  def code = "en"
}


object Languages {
  def apply(code: String) = 
    code match {
      case "fr" => fr
      case "en" => en
    }
}

case class Ballotz(title: String, instructions: String, 
    candidates: Seq[(String, Int)])

case class Argument(title: String, start: String, rest: String)

object CR {

  def evaluativeVoting = 
    fr("Vote Évaluatif") ~ 
    en("Evaluative Voting")

  def votingMethodThatEMpowersElectors = 
    fr("Rendre les élections aux électeurs") ~ 
    en("Elections for electors")

  def whyImplementAV = 
    fr("Pourquoi faut-il instaurer le vote évaluatif ?") ~
    en("Why do we need Evaluative Voting ?")

  def ballotTitle = 
    en("Approval voting Ballot") ~ 
    fr("Bulletin de vote par Approbation")

  def ballotInstruction = 
    en("Vote for one or more option", "") ~
    fr("", "Votez pour chaque candidat que vous approuvez, celui qui obtient le plus de vote gagne.")

  val ballotFr = Ballotz(
      "Bulltetin de Vote",
      "Votez pour chaque candidat que vous appouvez, le gagnant sera celui qui obtiendra le plus de vote.",
       Seq(
         "Le Parrain" -> -2,
         "Obelix" -> 1,
         "Gaston Lagaffe" -> -1,
         "Asterix" -> 2
       ))

  val ballotEn = Ballotz(
      "Voting Ballot",
      "Vote for as many candidate as you approve, the one with the most vote wins.",
       Seq(
         "Le Parrain" -> -2,
         "Obelix" -> -1,
         "Gaston Lagaffe" -> 0,
         "Gandalf" -> 3
       ))

  def convertToAv(b: Ballotz) =
    b.copy(candidates = b.candidates.map(c => (c._1, if(c._2 > 0) 1 else 0)))
       
  def exampleAVBallot = 
     fr.custom(convertToAv(ballotFr)) ~
     en.custom(convertToAv(ballotEn))
  
  def exampleRVBallot =
     fr.custom(ballotFr.copy(instructions = 
       "Évaluez chaque candidat avec un score <br> 2: excellent, 1: bon, 0: neutre, -1: mauvais, -2: très mauvais")) ~
     en.custom(ballotEn.copy(instructions = 
       "Evaluate each candidat with a grade <br> 2: Excellent, 1: Good, 0: Neutral, -1: Bad, -2: Very Bad"))

  def pop(s: String, id: String) = <a id={id}>{s}</a>

  def fbLang =
    fr("fr_CA") ~ en("en_CA")


  def end2PartyDomination = 
    fr.custom(Argument(
      "Mettre fin au <u>système à deux partis</u>",
      "Ce n'est pas un hazard si toutes les démocraties qui utilisent un système de vote uninominal élisent toujours les mêmes deux partis pendant des décénies, parfois des siècles,",
      "il s'agit d'une défectuosité mathématique de ce type système (démontré par Maurice Duvergé en 1950). " +
      "Le <b>vote évaluatif</b> s'il était instauré pourrait briser ce duopole."
    ))

  def solveVoteDivision = 
    fr.custom(Argument(
       "Voter sans craindre la <u>division du vote</u>",
       "À chaque élection, de nombreux électeurs ne votent pas pour l'option qu’ils préfèrent, par crainte de contribuer à la victoire d'un parti dont il craignent la prise de pouvoir",
       """
        Ils votent stratégiquement, c'est à dire pour un parti qu'ils n’approuvent pas nécéssairement, mais
        qu’ils considère être "moins pire" que le parti qu'ils craignent, et que les sondages prédisent être 
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
        
  def solveExtremistsHijacking0 =
     fr("Pour des partis politiques redevables à l'ensemble des électeurs et non uniquement à leur bases partisanes","""
        Le mode de scrutin uninominal fait en sorte que pour un parti politique, d’un point de vue stratégique, 
        les électeurs se divisent en deux catégories : ceux qui vote pour lui, et les autres.
        Un parti doit alors s’assurer que ses électeurs forment un majorité simple, si cette majorité est atteinte 
        il n’y a aucun coût électoral à aliéner les électeurs ne votant pas pour lui. C’est pour cette raison qu’il 
        arrive fréquement qu’un parti gouverne uniquement pour ses électeurs sans se préoccuper d'aliéner les autres.
        Avec le vote évaluatif, un parti doit considérer chaque électeurs sur un pied d’égalité, car chacun d’eux aura 
        l’occasion d’exprimer sur son bulletin de vote son approbation (ou désapprobation).
        """) ~
     en("So that parties receptive to all citizens, not only their partisan base","""
        Under a uninominal voting system, from a strategic perspective, political parties view electors in two categories :
        those that vote for them, and those that don't.
        When the electorate is divided, a party that has enough electors to win can becomes immune to dissatisfaction outside 
        his electorate, no matter how intense. With Evaluative Voting all parties are evaluated by all electors with a score of approval.
        It encourages parties to respect for all electors, but it
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


