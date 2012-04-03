package models

import org.squeryl._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.CompositeKey2
import java.sql.DriverManager
import org.squeryl.adapters.H2Adapter


case class ElectionMethod(title: String, description: String)