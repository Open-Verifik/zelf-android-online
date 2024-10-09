package co.verifik.wallet.utils

class DataHolder {
    companion object {
        // Singleton instance
        private val instance: DataHolder = DataHolder()

        // Method to get the singleton instance
        public fun getInstance(): DataHolder {
            return instance
        }
    }

    private val extras: MutableMap<String, Any> = HashMap()

    public fun putExtra(
        name: String,
        obj: Any,
    ) {
        extras[name] = obj
    }

    public fun getExtra(name: String): Any? {
        return extras[name]
    }

    public fun hasExtra(name: String): Boolean {
        return extras.containsKey(name)
    }

    public fun clear() {
        extras.clear()
    }
}
