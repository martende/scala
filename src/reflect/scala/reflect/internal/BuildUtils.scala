package scala.reflect
package internal

trait BuildUtils { self: SymbolTable =>

  class BuildImpl extends BuildApi {

    def selectType(owner: Symbol, name: String): TypeSymbol =
      select(owner, newTypeName(name)).asType

    def selectTerm(owner: Symbol, name: String): TermSymbol = {
      val result = select(owner, newTermName(name)).asTerm
      if (result.isOverloaded) result.suchThat(!_.isMethod).asTerm
      else result
    }

    private def select(owner: Symbol, name: Name): Symbol = {
      val result = owner.info decl name
      if (result ne NoSymbol) result
      else
        mirrorThatLoaded(owner).missingHook(owner, name) orElse
        MissingRequirementError.notFound("%s %s in %s".format(if (name.isTermName) "term" else "type", name, owner.fullName))
    }

    def selectOverloadedMethod(owner: Symbol, name: String, index: Int): MethodSymbol = {
      val result = owner.info.decl(newTermName(name)).alternatives(index)
      if (result ne NoSymbol) result.asMethod
      else MissingRequirementError.notFound("overloaded method %s #%d in %s".format(name, index, owner.fullName))
    }

    def newFreeTerm(name: String, value: => Any, flags: Long = 0L, origin: String = null): FreeTermSymbol =
      newFreeTermSymbol(newTermName(name), value, flags, origin)

    def newFreeType(name: String, flags: Long = 0L, origin: String = null): FreeTypeSymbol =
      newFreeTypeSymbol(newTypeName(name), flags, origin)

    def newNestedSymbol(owner: Symbol, name: Name, pos: Position, flags: Long, isClass: Boolean): Symbol =
      owner.newNestedSymbol(name, pos, flags, isClass)

    def setAnnotations[S <: Symbol](sym: S, annots: List[AnnotationInfo]): S =
      sym.setAnnotations(annots)

    def setTypeSignature[S <: Symbol](sym: S, tpe: Type): S =
      sym.setTypeSignature(tpe)

    def flagsFromBits(bits: Long): FlagSet = bits

    def This(sym: Symbol): Tree = self.This(sym)

    def Select(qualifier: Tree, sym: Symbol): Select = self.Select(qualifier, sym)

    def Ident(sym: Symbol): Ident = self.Ident(sym)

    def TypeTree(tp: Type): TypeTree = self.TypeTree(tp)

    def thisPrefix(sym: Symbol): Type = sym.thisPrefix

    def setType[T <: Tree](tree: T, tpe: Type): T = { tree.setType(tpe); tree }

    def setSymbol[T <: Tree](tree: T, sym: Symbol): T = { tree.setSymbol(sym); tree }

    object FlagsAsBits extends FlagsAsBitsExtractor {
      def unapply(flags: Long): Option[Long] = Some(flags)
    }

    object EmptyValDefLike extends EmptyValDefExtractor {
      def unapply(t: Tree): Boolean = t eq emptyValDef
    }

    object PendingSuperCallLike extends PendingSuperCallExtractor {
      def unapply(t: Tree): Boolean = t eq pendingSuperCall
    }

    object Applied extends AppliedExtractor {
      def unapply(tree: Tree): Option[(Tree, List[Tree], List[List[Tree]])] = tree match {
        case treeInfo.Applied(fun, targs, argss) => Some((fun, targs, argss))
        case _ => None
      }
    }

    object Applied2 extends Applied2Extractor {
      def unapply(tree: Tree): Option[(Tree, List[List[Tree]])] = tree match {
        case treeInfo.Applied(fun, targs, argss) =>
          if(targs.length > 0)
            Some((TypeApply(fun, targs), argss))
          else
            Some((fun, argss))
        case _ => None
      }
    }
  }

  val build: BuildApi = new BuildImpl
}
