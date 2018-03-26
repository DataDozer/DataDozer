///<reference path="datadozer.d.ts" />

let sort: Sort = (p, sap, score) =>
    ((0.3 * sap / 10.0) + (0.7 * score))

let scorer: Scorer = (p: Properties, sap, score) =>
    ((0.3 * sap / 10.0) + (0.7 * score))

let column: Column = (p: Properties, d: Document) => {
    if (p.getBoolValue("sap")) {
        return ""
    } else {
        return ""
    }
}

/**
 * Style 1
 * In this style a user can specify all the parameters that is required.
 * 
 * Advantages
 * - Signature tells what is needed
 * - Can specifiy the default values for the columns
 * - Easier model as user does not have to get the values out of the dictionary
 * - Less error prone as null checking is done for the user
 * - Still extremely concise
 * - Convention based, any variable starting with _ comes from the properties bag and 
 *   everything else comes from document 
 * Disadvantages
 * - Requires more work on our part to code things
 */
let scorer1: Scorer = (_biasFactor: double = 1, _score: number = 1.0, popularity: double[] = [1, 0]) =>
    Math.sqrt(_score) + Math.log(_biasFactor + popularity[0])

/**
 * Style 2
 * In this style we pass user two parameters: Document and Properties
 * 
 * Disadvantages
 * - User has to get all the values out of the dictionary
 * - More error prone
 * - No way to provide default value
 */
let scorer2: Scorer = (p: Properties, d: Document) =>
    Math.sqrt(p.getDoubleValue("score"))
    + Math.log(p.getDoubleValue("biasFactor") + d.getLongValue("popularity"))