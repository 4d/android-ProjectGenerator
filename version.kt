
import com.github.ajalt.clikt.core.CliktCommand

class Version: CliktCommand(name = "version") {
    override fun run() {
        echo("VERSION")
    }
}
