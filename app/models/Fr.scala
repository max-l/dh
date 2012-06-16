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

object CR {

  def evaluativeVoting = 
    fr("Le vote Évaluatif") ~ 
    en("Evaluative Voting")

  def votingMethodThatEMpowersElectors = 
    fr("Rendre les élections aux électeurs") ~ 
    en("Elections for electors")

  def whyImplementAV = 
    fr("Pourquoi faut-il instaurer le vote par approbation ?") ~
    en("Why do we need Approval Voting ?")

  def ballotTitle = 
    en("Approval voting Ballot") ~ 
    fr("Bulletin de vote par Approbation")

  def ballotInstruction = 
    en("Vote for one or more option") ~ 
    fr("Votez pour un ou plusieurs candidats")
    
  def pop(s: String, id: String) = <a id={id}>{s}</a>

  def top5Reasons = Seq(

     fr("Pour mettre fin aux \"système à deux partis\"",
        <p>
        Il a été démontré qu’un {pop("mode de scrutin uninominal","uninominalSystem")} crée un système à deux partis. Les nombreux problème 
        de ce mode de scrutin crée une barrière artificielle, protégeant ansi les deux partis de la concurrence. 
        L’innovation et la saine concurrence des idées est ansi étouffée. Le vote par approbation, en permettant 
        à l’électeur d’exprimer ses intentions sans entraves met fin à cette situation d’oligopole.
        </p>) ~
     en("To put an end to the \"Two party System\"",
        <p>
        {pop("Uninominal voting systems","uninominalSystem")} are the main cause of for two party systems. It effectively creates 
        an artificial barrier so high that the two parties are practically immune from competition.
        Approval Voting can put an end to this duopoly.
        </p>),

     fr("Pour pouvoir voter honnêtement, et non stratégiquement", """
        À chaque élection, de nombreux électeurs ne votent pas pour l'option qu’ils préfèrent, par crainte de 
        contribuer à la victoire d'un parti dont il craignent la prise de pouvoir. Ils votent stratégiquement, 
        c'est à dire pour un parti qu'ils n’approuvent pas nécéssairement, mais
        qu’ils considère être "moins pire" que le parti qu'ils craignent, et que les sondages prédisent être 
        le plus susceptible de gagner. Un électeur qui est contraint de voter stratégiquement, perd en quelque 
        sorte le droit d’exprimer son intention réelle. Avec le vote par approbation, l'électeur n'est jamais 
        pénalisé en votant selon ses convictions.
        """) ~
     en("So that honnest voting is not penalized","""
        Many electors don't vote according to their true intents, by fear of contributing to elect an unwanted
        candidate, the are forced to vote for _strategically_ for the candidate that they don't necessarily like,
        but that is "not as bad" as the candidate they fear, and who is deemed electable by surveys.
        A voter that is forced into strategic voting looses is right to express his real intent. With approval voting
        one can vote honestly without penalty.
        """),

     fr("Pour que l'électeur puisse évaluer chaque choix de façon indépendante avec son vote","""
        La libre expression est essentielle à la démocratie. L’élection est l’ultime exercice de ce droit d’expression, 
        car c’est le seul moment où ce qui est exprimé (par les électeurs) a un impact direct sur le pouvoir. 
        Un système électoral doit être évalué sur sa capacité véhiculer l’intention de l’électeur. Sur ce plan, 
        le mode de scrutin uninominal est extrèmement faible : il force l’électeur à donner un appui complet à un seul 
        candidat, et un rejet total de tous les autres. Le vote par approbation, par sa plus grande expressivité, 
        donne à l’électeur le pouvoir qui lui est du.
        """) ~
     en("To give electors a change to evaluate each option *independently*","""
        Freedom of expression is an essential part of democracy. An election is the ultimate exercise of this right,
        it is the only time that a citizen's expression has a direct effect on the power by which he is governed.
        A voting method is only as good as it's capacity to communicate the intentions of the electors.
        Uninominal voting systems are extremely poor in that regard : it forces an elector to give the maximum
        approval to a single option, and reject all others equally. The expressiveness of an Approval Vote gives
        the elector the power that is rightfuly his.
        """),

     fr("Pour des partis politiques redevables à l'ensemble des électeurs et non uniquement à leur bases partisanes","""
        Le mode de scrutin uninominal fait en sorte que pour un parti politique, d’un point de vue stratégique, 
        les électeurs se divisent en deux catégories : ceux qui vote pour lui, et les autres.
        Un parti doit alors s’assurer que ses électeurs forment un majorité simple, si cette majorité est atteinte 
        il n’y a aucun coût électoral à aliéner les électeurs ne votant pas pour lui. C’est pour cette raison qu’il 
        arrive fréquement qu’un parti gouverne uniquement pour ses électeurs sans se préoccuper d'aliéner les autres.
        Avec le vote par approbation, un parti doit considérer chaque électeurs sur un pied d’égalité, car chacun d’eux aura 
        l’occasion d’exprimer sur son bulletin de vote son approbation (ou désapprobation).
        """) ~
     en("So that parties receptive to all citizens, not only their partisan base","""
        Under a uninominal voting system, from a strategic perspective, political parties view electors in two categories :
        those that vote for them, and those that don't.
        When the electorate is divided, a party that has enough electors to win can becomes immune to dissatisfaction outside 
        his electorate, no matter how intense. With Approval Voting all parties are evaluated by all electors with a score of approval.
        It encourages parties to respect for all electors, but it
        """),

     fr("Parce qu’une démocratie malade crée les conditions de son déclin","""
        Entre la démocratie idéale et la dictature, il y a une grande zone grise. Il est plus exact de parler de santé 
        démocratique simplement la déclarer absente ou présente. Une démocratie est malade lorsqu’un groupe d'élus réeussent à 
        accroître leur pouvoirs au delà de ce que lui accorde l’électorat. Une telle situation crée de nombreuses occasions 
        permettant à ce groupe de consolider son pouvoir au détriment de l’électorat, fermant ainsi la boucle d’un cercle vicieux. 
        Le vote par approbation, en rétablissant l’équilibre en faveur des électeurs, 
        permettrait d’inverser le déclin démocratique actuel.
        """) ~
     en("Because a weakened democracy creates the conditions for it's decline","""
        Between democracy and dictatorship, there is a wide gray area. It is more precise to speak of democratic health rather than
        it's presense or absence. A democracy is weakend when politicians obtain more power than those granted by the electorate,
        it is a slipery slope : these politicians can use powers to obtain more powers, closing the loop of a vicous circle.
        Approval Voting could help invert this democratic decline.
        """)
  )
}


