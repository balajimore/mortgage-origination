package net.synechron.cordapp.morigin.plugin

import net.corda.core.serialization.SerializationWhitelist

class SerializationWhitelistImpl : SerializationWhitelist{
    override val whitelist: List<Class<*>>
        get() = emptyList()
}