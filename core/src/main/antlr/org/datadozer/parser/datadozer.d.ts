declare function double(a): number

declare type DocString = string
declare type DocInteger = number
declare type DocLong = number
declare type DocFloat = number
declare type DocDouble = number
declare type DocBool = boolean
declare type DocStrings = string[]
declare type DocIntegers = number[]
declare type DocFloats = number[]
declare type DocDecimals = number[]
declare type DocLongs = number[]

declare type PropString = string
declare type PropInteger = number
declare type PropLong = number
declare type PropFloat = number
declare type PropDouble = number
declare type PropBool = boolean

declare type double = number


declare interface Properties {
    getStringValue(param: string): string
    getIntegerValue(param: string): number
    getLongValue(param: string): number
    getFloatValue(param: string): number
    getDoubleValue(param: string): number
    getBoolValue(param: string): boolean
    getIntegerValues(param: string): number[]
    getFloatValues(param: string): number[]
    getLongValues(param: string): number[]
    getDoubleValues(param: string): number[]
    getStringValues(param: string): string
    set(key: string, value: string)
}


declare interface Document {
    getStringValue(param: string): string
    getIntegerValue(param: string): number
    getLongValue(param: string): number
    getFloatValue(param: string): number
    getDoubleValue(param: string): number
    getBoolValue(param: string): boolean
    getIntegerValues(param: string): number[]
    getFloatValues(param: string): number[]
    getLongValues(param: string): number[]
    getDoubleValues(param: string): number[]
    getStringValues(param: string): string
    set(key: string, value: string)
}

declare type Double = number
declare class SearchQuery { }

interface Scorer {
    (...n: any[]): Double
}



interface Sort {
    (p: Properties, ...n: number[]): number
}

interface Column {
    (p: Properties, d: Document): String
}

interface PreSearchScript {
    (q: SearchQuery)
}