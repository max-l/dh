package models

import org.squeryl._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.CompositeKey2
import java.sql.DriverManager
import org.squeryl.adapters.H2Adapter


class PartType(val item        : String,
              val description : String,
              val unitPrice   : Int,
              val partNumber  : String) extends KeyedEntity[Int] {
  def this() = this("", "", 0, "")

  val id : Int = 0
}

class PowerRating(val power: PowerRating.PowerRating,
                  val description : String) extends KeyedEntity[PowerRating.PowerRating] {
  def this() = this(PowerRating.KW160, "")
  def id = power
}

class BillOfMaterial(val powerRatingId : PowerRating.PowerRating,
                    val partTypeId    : Int,
                    val quantity      : Int) extends KeyedEntity[CompositeKey2[PowerRating.PowerRating, Int]] {
 val id = compositeKey(powerRatingId, partTypeId)
}  


object PowerRating extends Enumeration {
  type PowerRating = Value
  val KW25  = Value(25000)
  val KW50  = Value(50000)
  val KW100 = Value(100000)
  val KW160 = Value(160000)
}

object ScratchPad extends Schema {

  val partType = table[PartType]
  on(partType)(x => declare(
     x.id     is(primaryKey, unique, indexed)
  ))

  val powerRating = table[PowerRating]
  on(powerRating)(x => declare(
     x.id        is(primaryKey, unique, indexed)
  ))

 // the ManyToMany associating the above two
  val billOfMaterials =
     manyToManyRelation(powerRating, partType)
     .via[BillOfMaterial]((r, p, bom) => (r.id === bom.powerRatingId, p.id === bom.partTypeId))


  def main(args: Array[String]): Unit = {

    Class.forName("org.h2.Driver")
    
    SessionFactory.concreteFactory = Some(() =>
      org.squeryl.Session.create(DriverManager.getConnection("jdbc:h2:mem:test"), new H2Adapter))
      
   
    ScratchPad.create
  }
}

