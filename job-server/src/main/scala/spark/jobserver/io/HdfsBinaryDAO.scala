package spark.jobserver.io

import com.typesafe.config.Config
import org.apache.commons.io.IOUtils.toByteArray
import org.slf4j.LoggerFactory
import spark.jobserver.util.{HadoopFSFacade, Utils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Data access object for retrieving/persisting binary data on HDFS.
  * @param config config of jobserver
  */
class HdfsBinaryDAO(config: Config) extends BinaryDAO {
  private val logger = LoggerFactory.getLogger(getClass)

  private val binaryBasePath =
    config.getString("spark.jobserver.combineddao.binarydao.dir").stripSuffix("/")
  private val hdfsFacade = new HadoopFSFacade()

  override def validateConfig(config: Config): Boolean = {
    config.hasPath("spark.jobserver.combineddao.binarydao.dir")
  }

  override def save(id: String, binaryBytes: Array[Byte]): Future[Boolean] = {
    Future {
      hdfsFacade.save(extendPath(id), binaryBytes, skipIfExists = true)
    }
  }

  override def delete(id: String): Future[Boolean] = {
    Future {
      hdfsFacade.delete(extendPath(id))
    }
  }

  override def get(id: String): Future[Option[Array[Byte]]] = {
    Future {
      hdfsFacade.get(extendPath(id)) match {
        case Some(bytesArray) =>
          Some(Utils.usingResource(bytesArray)(toByteArray))
        case None =>
          logger.error(s"Failed to get a file $id from HDFS.")
          None
      }
    }
  }

  private def extendPath(id: String): String = s"$binaryBasePath/$id"
}
