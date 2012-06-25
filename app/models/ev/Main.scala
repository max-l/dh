package models.ev


case class Ballotz(title: String, instructions: String, 
    candidates: Seq[(String, Int)])

case class Argument(title: String, start: String, rest: String)

case class Intro(short: String, longer: String, completeDesc: String)

case class ThreeVariations(preamble: String, approval: Intro, range: Intro, majorityJudgement: Intro, link: String)


object Main {

  def evaluativeVoting = 
    fr("Vote Évaluatif") ~ 
    en("Evaluation Voting")

  def citizendForEvaluativeVoting = 
    fr("Un regroupement citoyen pour l'instauration du") ~ 
    en("Citizens for the implementation of")

  def invertTheDemocraticDeficitWHileWeStillCan = 
    fr("Inversons le dégradation de la démocratie, pendant que c'est encore possible.") ~ 
    en("Reverse the degradation of democracy while we still can.")

  def definitionOfEV = 
     fr("""<p class='defn'><span>Vote Évaluatif</span> : un système de vote où l'électeur évalue chaque candidat à l'aide d'un score,
        le gagnant est celui qui obtient le meilleur score cumulatif.
     </p>""") ~
     en("""<p class='defn'><span>Evaluation Voting</span> : a voting system in which the elector evaluates each option independently with a 
        grade, the one with the best cumulative grade wins.
     </p>""")
  def why = 
    fr("Pourquoi ?") ~ 
    en("Why ?")

  val ballotFr = Ballotz(
      "Bulletin de Vote",
      "Votez pour chaque candidat que vous appouvez, le gagnant sera celui qui obtiendra le plus de vote.",
       Seq(
         "Le Parrain" -> -2,
         "Obélix" -> 1,
         "Gaston Lagaffe" -> -1,
         "Astérix" -> 2
       ))

  val ballotEn = Ballotz(
      "Voting Ballot",
      "Vote for as many candidate as you approve, the one with the most vote wins.",
       Seq(
         "Gandalf" -> 2,
         "Darth Vader" -> -2,
         "Bart Simpson" -> 1,
         "Homer Simpson" -> -1
       ))

  def convertToAv(b: Ballotz) =
    b.copy(candidates = b.candidates.map(c => (c._1, if(c._2 > 0) 1 else 0)))
       
  def exampleAVBallot = 
     fr.custom(convertToAv(ballotFr)) ~
     en.custom(convertToAv(ballotEn))

  def threeVariations = 
     fr.custom(ThreeVariations(
         "Trois variantes du vote évaluatif",
         Intro("Approbation","Le vote par Approbation",
           """
            L'électeur vote pour autant de candidat qu'il approuve. <br/>Le fait qu'il soit possible de voter pour 
            <b>plus d'un</b> candidat, fait en sorte que l'électeur puisse toujours voter pour celui ou ceux qu'il approuve.
            """),
         Intro("Par Valeur","Le vote par Valeur",
           """
            L'électeur donne un score entre -2 et +2 à chacun des candidats, celui dont la <b>somme</b> des scores est la plus élevée gagne.<br>
            Ce système de vote est hautement <b>expressif</b>.
            """),         
         Intro("Jugement Majoritaire","Le Jugement Majoritaire",
           """
            L'électeur donne un score entre -2 et +2 à chacun des candidats, celui dont la <b>médiane</b> des scores est la plus élevée gagne.
            Comme le vote par valeur, ce système de vote est hautement <b>expressif</b>.
            """),
         "Voir <a target='_blank' href='http://www.votedevaleur.org/'>http://www.votedevaleur.org/</a>, pour plus d'info sur le vote par valeur."
     )) ~
     en.custom(ThreeVariations(
         "Three kinds of Evaluation Voting",
         Intro("Approval","Approval Voting",
           """
            The elector votes for as many candidates as he likes. The ability to vote for <b>more than one</b> has the
            consequence that one can always vote for a candidate that he approve of.
            """),
         Intro("Score Voting","Score Voting",
           """
            The elector gives a score ranging from -2 to +2 to each candidates, the one with the highest <b>sum</b> wins.
            It is a highly <b>expressive</b> voting method.
            """),         
         Intro("Majority Judgment","Majority Judgement",
           """
            The elector gives a score ranging from -2 to +2 to each candidates, the one with the highest <b>median</b> wins.
            Like score voting, it is a highly <b>expressive</b> voting method.
            """),
         "See <a target='_blank' href='http://scorevoting.net'>http://scorevoting.net</a> for more info on score voting"
     ))
     
  def exampleRVBallot =
     fr.custom(ballotFr.copy(instructions = 
       "Évaluez chaque candidat avec un score <br> 2: excellent, 1: bon, 0: neutre, -1: mauvais, -2: très mauvais")) ~
     en.custom(ballotEn.copy(instructions = 
       "Evaluate each candidat with a grade <br> 2: Excellent, 1: Good, 0: Neutral, -1: Bad, -2: Very Bad"))

  def pop(s: String, id: String) = <a id={id}>{s}</a>

  def fbLang =
    fr("fr_CA") ~ en("en_US")
}
