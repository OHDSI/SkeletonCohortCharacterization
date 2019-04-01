#@file Main.R

#' This package is the skeleton to build own packages with Cohort Characterization analyses. That package is able to run at any site that has access to an observational database in the Common Data Model.
#'
#' @docType package
#' @name SkeletonCohortCharacterization
NULL

.onLoad <- function(libname, pkgname) {
  rJava::.jpackage(pkgname, lib.loc = libname)
}
